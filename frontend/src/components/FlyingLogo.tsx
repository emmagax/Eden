import { useEffect, useRef, useState } from "react";

const EDGE_MARGIN = 16;
const PANEL_CLEARANCE = 48;

/*
 * Random flight lasts 4000ms in CSS.
 * A new destination is selected after 4200ms.
 */
const MOVE_INTERVAL = 4200;
const MAX_RANDOM_ATTEMPTS = 50;
const PATH_STEPS = 24;

/*
 * Mouse-following physics.
 */
const FOLLOW_TICK = 180;
const STEERING_STRENGTH = 1;
const VELOCITY_DRAG = 0.5;
const WANDER_STRENGTH = 1;
const MAX_FOLLOW_SPEED = 80;

type Position = {
  x: number;
  y: number;
};

const INITIAL_POSITION: Position = {
  x: EDGE_MARGIN,
  y: EDGE_MARGIN,
};

function clamp(value: number, minimum: number, maximum: number) {
  return Math.min(Math.max(value, minimum), maximum);
}

function overlapsPanel(
  x: number,
  y: number,
  logoWidth: number,
  logoHeight: number,
  panelRect: DOMRect,
) {
  const logoLeft = x;
  const logoRight = x + logoWidth;
  const logoTop = y;
  const logoBottom = y + logoHeight;

  const panelLeft = panelRect.left - PANEL_CLEARANCE;

  const panelRight = panelRect.right + PANEL_CLEARANCE;

  const panelTop = panelRect.top - PANEL_CLEARANCE;

  const panelBottom = panelRect.bottom + PANEL_CLEARANCE;

  return (
    logoRight > panelLeft &&
    logoLeft < panelRight &&
    logoBottom > panelTop &&
    logoTop < panelBottom
  );
}

function pathOverlapsPanel(
  start: Position,
  destination: Position,
  logoWidth: number,
  logoHeight: number,
  panelRect: DOMRect,
) {
  for (let step = 1; step <= PATH_STEPS; step++) {
    const progress = step / PATH_STEPS;

    const x = start.x + (destination.x - start.x) * progress;

    const y = start.y + (destination.y - start.y) * progress;

    if (overlapsPanel(x, y, logoWidth, logoHeight, panelRect)) {
      return true;
    }
  }

  return false;
}

function getVisualPosition(element: HTMLElement): Position {
  const currentTransform = window.getComputedStyle(element).transform;

  const matrix =
    currentTransform === "none"
      ? new DOMMatrixReadOnly()
      : new DOMMatrixReadOnly(currentTransform);

  return {
    x: matrix.m41,
    y: matrix.m42,
  };
}

function FlyingLogo() {
  const logoRef = useRef<HTMLButtonElement>(null);

  const positionRef = useRef<Position>(INITIAL_POSITION);

  const velocityRef = useRef<Position>({
    x: 0,
    y: 0,
  });

  const pointerPositionRef = useRef<Position | null>(null);

  const pausedRef = useRef(false);
  const followingRef = useRef(false);

  /*
   * Allows the hover handlers to request a new random
   * target without recreating the movement effect.
   */
  const moveRandomlyRef = useRef<() => void>(() => {});

  const [position, setPosition] = useState<Position>(INITIAL_POSITION);

  const [isPaused, setIsPaused] = useState(false);

  const [isFollowing, setIsFollowing] = useState(false);

  /*
   * Always remember the latest pointer position.
   */
  useEffect(() => {
    function rememberPointerPosition(event: PointerEvent) {
      pointerPositionRef.current = {
        x: event.clientX,
        y: event.clientY,
      };
    }

    window.addEventListener("pointermove", rememberPointerPosition);

    return () => {
      window.removeEventListener("pointermove", rememberPointerPosition);
    };
  }, []);

  /*
   * Wander mode:
   *
   * - Select random destinations.
   * - Avoid the authentication panel.
   * - Stop while following.
   * - Stop while paused.
   */
  useEffect(() => {
    const reducedMotionPreference = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    );

    if (reducedMotionPreference.matches) {
      return;
    }

    function moveToRandomPosition() {
      if (pausedRef.current || followingRef.current) {
        return;
      }

      const logo = logoRef.current;

      if (!logo) {
        return;
      }

      const logoWidth = logo.offsetWidth;

      const logoHeight = logo.offsetHeight;

      const maximumX = window.innerWidth - logoWidth - EDGE_MARGIN;

      const maximumY = window.innerHeight - logoHeight - EDGE_MARGIN;

      if (maximumX <= EDGE_MARGIN || maximumY <= EDGE_MARGIN) {
        return;
      }

      const panel = document.querySelector<HTMLElement>(".auth-panel");

      const panelRect = panel?.getBoundingClientRect();

      for (let attempt = 0; attempt < MAX_RANDOM_ATTEMPTS; attempt++) {
        const destination: Position = {
          x: EDGE_MARGIN + Math.random() * (maximumX - EDGE_MARGIN),

          y: EDGE_MARGIN + Math.random() * (maximumY - EDGE_MARGIN),
        };

        const routeIsBlocked =
          panelRect !== undefined &&
          pathOverlapsPanel(
            positionRef.current,
            destination,
            logoWidth,
            logoHeight,
            panelRect,
          );

        if (!routeIsBlocked) {
          positionRef.current = destination;

          setPosition(destination);
          return;
        }
      }
    }

    moveRandomlyRef.current = moveToRandomPosition;

    moveToRandomPosition();

    const intervalId = window.setInterval(moveToRandomPosition, MOVE_INTERVAL);

    window.addEventListener("resize", moveToRandomPosition);

    return () => {
      moveRandomlyRef.current = () => {};

      window.clearInterval(intervalId);

      window.removeEventListener("resize", moveToRandomPosition);
    };
  }, []);

  /*
   * Follow mode:
   *
   * - Follow the current pointer position.
   * - Use acceleration and velocity.
   * - Do not use auth-panel collision.
   * - Remain inside the viewport.
   */
  useEffect(() => {
    if (!isFollowing) {
      velocityRef.current = {
        x: 0,
        y: 0,
      };

      return;
    }

    const logo = logoRef.current;

    if (!logo) {
      return;
    }

    const reducedMotionPreference = window.matchMedia(
      "(prefers-reduced-motion: reduce)",
    );

    if (reducedMotionPreference.matches) {
      return;
    }

    /*
     * Begin following from the logo's current visible
     * position, not its old random destination.
     */
    const visualPosition = getVisualPosition(logo);

    positionRef.current = visualPosition;

    setPosition(visualPosition);

    const intervalId = window.setInterval(() => {
      const pointer = pointerPositionRef.current;

      const currentLogo = logoRef.current;

      if (!pointer || !currentLogo) {
        return;
      }

      const logoWidth = currentLogo.offsetWidth;

      const logoHeight = currentLogo.offsetHeight;

      const maximumX = window.innerWidth - logoWidth - EDGE_MARGIN;

      const maximumY = window.innerHeight - logoHeight - EDGE_MARGIN;

      if (maximumX <= EDGE_MARGIN || maximumY <= EDGE_MARGIN) {
        return;
      }

      const current = positionRef.current;

      const velocity = velocityRef.current;

      const logoCenterX = current.x + logoWidth / 2;

      const logoCenterY = current.y + logoHeight / 2;

      /*
       * The pointer is an attraction point.
       */
      const distanceX = pointer.x - logoCenterX;

      const distanceY = pointer.y - logoCenterY;

      /*
       * Accelerate toward the pointer.
       */
      velocity.x += distanceX * STEERING_STRENGTH;

      velocity.y += distanceY * STEERING_STRENGTH;

      /*
       * Add a small amount of random motion so the
       * follower still looks like it is flying.
       */
      velocity.x += (Math.random() - 0.5) * WANDER_STRENGTH;

      velocity.y += (Math.random() - 0.5) * WANDER_STRENGTH;

      /*
       * Apply inertia/air resistance.
       */
      velocity.x *= VELOCITY_DRAG;
      velocity.y *= VELOCITY_DRAG;

      /*
       * Limit the maximum speed.
       */
      const speed = Math.hypot(velocity.x, velocity.y);

      if (speed > MAX_FOLLOW_SPEED) {
        const scale = MAX_FOLLOW_SPEED / speed;

        velocity.x *= scale;
        velocity.y *= scale;
      }

      const requestedX = current.x + velocity.x;

      const requestedY = current.y + velocity.y;

      const nextX = clamp(requestedX, EDGE_MARGIN, maximumX);

      const nextY = clamp(requestedY, EDGE_MARGIN, maximumY);

      /*
       * Bounce gently at viewport boundaries.
       */
      if (nextX !== requestedX) {
        velocity.x *= -0.5;
      }

      if (nextY !== requestedY) {
        velocity.y *= -0.5;
      }

      const nextPosition: Position = {
        x: nextX,
        y: nextY,
      };

      positionRef.current = nextPosition;

      setPosition(nextPosition);
    }, FOLLOW_TICK);

    return () => {
      window.clearInterval(intervalId);

      velocityRef.current = {
        x: 0,
        y: 0,
      };
    };
  }, [isFollowing]);

  /*
   * Hover pause applies only during wander mode.
   *
   * It cannot apply while following because the logo
   * will eventually reach the pointer and immediately
   * pause itself.
   */
  function handlePointerEnter() {
    if (followingRef.current) {
      return;
    }

    const logo = logoRef.current;

    if (!logo) {
      return;
    }

    pausedRef.current = true;

    const visualPosition = getVisualPosition(logo);

    /*
     * Freeze both the transition and transform
     * synchronously before React rerenders.
     */
    logo.style.transition = "none";

    logo.style.transform = `translate(${visualPosition.x}px, ${visualPosition.y}px)`;

    positionRef.current = visualPosition;

    setPosition(visualPosition);
    setIsPaused(true);
  }

  function handlePointerLeave() {
    if (followingRef.current) {
      return;
    }

    const logo = logoRef.current;

    pausedRef.current = false;
    setIsPaused(false);

    if (logo) {
      logo.style.removeProperty("transition");
    }

    /*
     * Resume random flight immediately rather than
     * waiting for the next interval.
     */
    window.setTimeout(() => {
      moveRandomlyRef.current();
    }, 0);
  }

  /*
   * Toggle:
   *
   * wander -> follow
   * follow -> wander
   */
  function handleLogoClick() {
    const shouldFollow = !followingRef.current;

    followingRef.current = shouldFollow;

    setIsFollowing(shouldFollow);

    const logo = logoRef.current;

    if (logo) {
      logo.style.removeProperty("transition");
    }

    if (shouldFollow) {
      /*
       * The click began while the logo was hovered and
       * paused. Following must resume movement.
       */
      pausedRef.current = false;
      setIsPaused(false);
    } else {
      /*
       * After the second click, the pointer remains
       * over the logo. Pause until pointerleave.
       */
      pausedRef.current = true;
      setIsPaused(true);
    }
  }

  const logoClassName = [
    "flying-logo",

    isPaused ? "flying-logo--paused" : "",

    isFollowing ? "flying-logo--following" : "",
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <button
      ref={logoRef}
      type="button"
      className={logoClassName}
      aria-label={
        isFollowing
          ? "Return Eden logo to random flight"
          : "Make Eden logo follow the pointer"
      }
      aria-pressed={isFollowing}
      onClick={handleLogoClick}
      onPointerEnter={handlePointerEnter}
      onPointerLeave={handlePointerLeave}
      style={{
        transform: `translate(${position.x}px, ${position.y}px)`,
      }}
    >
      <span className="logo-mark" aria-hidden="true">
        <span className="logo-wing logo-wing-left">{"{"}</span>

        <span className="logo-center">*</span>

        <span className="logo-wing logo-wing-right logo-tight">{"}"}</span>
      </span>
    </button>
  );
}

export default FlyingLogo;
