package in.picturehouse.jaipur.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;

@Entity @Table(name="accounts", uniqueConstraints={@UniqueConstraint(columnNames="email"),@UniqueConstraint(columnNames="mobile")})
public class Account {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Email private String email;
    private String mobile;
    private String displayName;
    @Enumerated(EnumType.STRING) private Role role=Role.CUSTOMER;
    private boolean emailVerified;
    private boolean mobileVerified;
    protected Account() {}
    public Account(String displayName,String email,String mobile,Role role){this.displayName=displayName;this.email=email;this.mobile=mobile;this.role=role;}
    public Long getId(){return id;} public String getEmail(){return email;} public String getMobile(){return mobile;} public String getDisplayName(){return displayName;} public Role getRole(){return role;} public boolean isEmailVerified(){return emailVerified;} public boolean isMobileVerified(){return mobileVerified;}
}
