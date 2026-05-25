package at.qe.skeleton.models;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Entity representing users.
 * This class is part of the skeleton project provided for students of the course "Software
 * Engineering" offered by Innsbruck University.
 */
@Getter
@Setter
@Entity
public class Userx implements Persistable<Long>, Serializable, Comparable<Userx>, UserDetails {
    @Serial
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Userx createUser;
    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime createDate;
    @ManyToOne(fetch = FetchType.LAZY)
    private Userx updateUser;
    @UpdateTimestamp
    private LocalDateTime updateDate;

    @Column(unique = true, nullable = false, length = 100)
    private String username;
    private String password;

    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    @ElementCollection(targetClass = UserxRole.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "Userx_UserxRole")
    @Enumerated(EnumType.STRING)
    private Set<UserxRole> roles;

    @SuppressWarnings("java:S1948")
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Absence> absences = new ArrayList<>();

    @SuppressWarnings("java:S1948")
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private EmployeeProfile employeeProfile;

    boolean enabled;

    @Override
    public boolean isAccountNonExpired() {
      return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
      return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
      return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return getRoles();
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 59 * hash + Objects.hashCode(this.getId());
      return hash;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof Userx other)) {
        return false;
      }
        return Objects.equals(this.getId(), other.getId());
    }

    @Override
    public String toString() {
      return "at.qe.skeleton.model.User[ id=" + username + " ]";
    }

    @Override
    public Long getId() {
      return id;
    }

    @Override
    public boolean isNew() {
      return (null == id);
    }

    @Override
    public int compareTo(Userx o) {
        if (o.getId() != null) {
          return this.id.compareTo(o.getId());
        }
        return 0;
    }

  }
