package com.emmagax.coro.model;

import jakarta.persistence.*;

@Entity
@Table(name = "connection_requests")
public class ConnectionRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "from_profile_id", nullable = false)
    private Profile fromProfile;

    @ManyToOne
    @JoinColumn(name = "to_profile_id", nullable = false)
    private Profile toProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    public ConnectionRequest() {}

    public Long getId() {
        return id;
    }
    public void setId(Long id) { this.id = id; }

    public Profile getFromProfile() {
        return fromProfile;
    }
    public void setFromProfile(Profile fromProfile) {
        this.fromProfile = fromProfile;
    }

    public Profile getToProfile() {
        return toProfile;
    }
    public void setToProfile(Profile toProfile) {
        this.toProfile = toProfile;
    }

    public RequestStatus getStatus() {
        return status;
    }
    public void setStatus(RequestStatus status) {
        this.status = status;
    }


}
