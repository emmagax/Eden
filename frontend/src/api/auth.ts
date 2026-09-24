export type AuthUser = {
  id: number;
  email: string;
  username: string;
};

export type RegisterRequest = {
  email: string;
  username: string;
  password: string;
};

export type LoginRequest = {
  identifier: string;
  password: string;
};

type ApiErrorResponse = {
  message?: string;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export async function register(request: RegisterRequest): Promise<AuthUser> {
  return sendAuthRequest("/auth/register", request);
}

export async function login(request: LoginRequest): Promise<AuthUser> {
  return sendAuthRequest("/auth/login", request);
}

async function sendAuthRequest<TRequest>(path: string, request: TRequest): Promise<AuthUser> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "include",
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    throw new Error(await getErrorMessage(response));
  }

  return response.json();
}

async function getErrorMessage(response: Response): Promise<string> {
  try {
    const error = (await response.json()) as ApiErrorResponse;
    return error.message ?? "Something went wrong. Please try again.";
  } catch {
    return "Something went wrong. Please try again.";
  }
}
