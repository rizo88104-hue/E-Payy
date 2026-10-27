package uz.epay.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "brands")
@Getter
@Setter
@NoArgsConstructor
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /** Havola uchun: "dell", "hpe" ... (kichik lotin harf, raqam va "-"). */
    @Column(nullable = false, unique = true, length = 64)
    private String slug;

    /** Logotip havolasi, masalan /images/brands/dell.svg; bo'sh bo'lsa nomi yoziladi. */
    private String logo;

    @Column(length = 1000)
    private String description;

    /** Kelib chiqqan mamlakat, masalan "AQSh". */
    @Column(length = 100)
    private String country;

    /** Tashkil topgan yili. */
    private Integer founded;

    /** Rasmiy sayt, masalan https://www.dell.com */
    private String website;
}
