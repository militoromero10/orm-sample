package orm.model;

import orm.annotations.Column;
import orm.annotations.Id;
import orm.annotations.Table;

import java.time.LocalDateTime;

@Table("person")
public class Persona {

    @Id
    @Column("id")
    private Long id;
    @Column("firstName")
    private String name;
    @Column("lastName")
    private String lastName;
    @Column("age")
    private Integer age;
    @Column("birthDate")
    private LocalDateTime birthDate;

    public Persona(String name, String lastName, Integer age, LocalDateTime birthDate) {
        this.id = 0L;
        this.name = name;
        this.lastName = lastName;
        this.age = age;
        this.birthDate = birthDate;
    }

    public Persona(final long id, final String name, final String lastName, final Integer age, final LocalDateTime birthDate) {
        this(name, lastName, age, birthDate);
        this.id = id;
    }

    @Override
    public String toString() {
        return "Persona{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lastName='" + lastName + '\'' +
                ", age=" + age +
                ", birthDate=" + birthDate +
                '}';
    }
}
