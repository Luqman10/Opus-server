package com.samaritan.opus.model;

import com.google.gson.annotations.Expose;
import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "payment_account", schema = "opus")
public class PaymentAccount {
    
    //data fields
    @Expose(serialize = true, deserialize = true)
    @Id
    @Column(name = "id", nullable = false, unique = true)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id ;
    
    @Expose(serialize = true, deserialize = true)
    @OneToOne
    @JoinColumn(name="profile_account", unique = true,nullable = true)
    private ProfileAccount profileAccount ;
    
    @Expose(serialize = true, deserialize = true)
    @Column(name = "phone_number", nullable = false, length = 11)
    private String phoneNumber;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    
    public ProfileAccount getProfileAccount() {
        return profileAccount;
    }

    public void setProfileAccount(ProfileAccount profileAccount) {
        this.profileAccount = profileAccount ;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentAccount that = (PaymentAccount) o;
        return  Objects.equals(id, that.id) &&
                Objects.equals(profileAccount, that.profileAccount) &&
                Objects.equals(phoneNumber, that.phoneNumber) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,profileAccount, phoneNumber);
    }
}
