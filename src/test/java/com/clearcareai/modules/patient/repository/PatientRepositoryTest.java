package com.clearcareai.modules.patient.repository;

import com.clearcareai.modules.auth.entity.User;
import com.clearcareai.modules.auth.repository.UserRepository;
import com.clearcareai.modules.patient.entity.Patient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class PatientRepositoryTest {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    // used only by the last test, to read the raw column value
    @PersistenceContext
    private EntityManager entityManager;

    private User savedUser;

    @BeforeEach
    void setUp() {
        // every test needs a User to hang the profile off
        User user = User.builder()
                .email("rahul@example.com")
                .password("$2a$10$fakehashfakehashfakehashfakehashfakehashfake")
                .firstName("Rahul")
                .lastName("Sharma")
                .phone("9876543210")
                .role(User.Role.ROLE_PATIENT)
                .build();
        savedUser = userRepository.save(user);
    }

    // a Patient with everything filled in, not yet saved
    private Patient buildPatient(User user) {
        return Patient.builder()
                .user(user)
                .dateOfBirth(LocalDate.of(1998, 5, 15))
                .gender(Patient.Gender.MALE)
                .bloodGroup("O+")
                .address("123 Main Street, Hyderabad")
                .medicalHistory("No known allergies")
                .build();
    }

    @Test
    void save_assignsAnIdAndBothTimestamps() {
        Patient patient = buildPatient(savedUser);

        Patient saved = patientRepository.save(patient);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUser().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    void findByUserId_returnsTheProfile_whenOneExists() {
        patientRepository.save(buildPatient(savedUser));

        Optional<Patient> found = patientRepository.findByUserId(savedUser.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getBloodGroup()).isEqualTo("O+");
        assertThat(found.get().getGender()).isEqualTo(Patient.Gender.MALE);
    }

    // THIS is the D4 case: a registered user who has not created a profile yet.
    // It must return an empty Optional, not throw and not return null.
    @Test
    void findByUserId_returnsEmpty_whenTheUserHasNoProfile() {
        Optional<Patient> found = patientRepository.findByUserId(savedUser.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void existsByUserId_isTrueOnlyWhenAProfileExists() {
        boolean beforeSaving = patientRepository.existsByUserId(savedUser.getId());

        patientRepository.save(buildPatient(savedUser));
        boolean afterSaving = patientRepository.existsByUserId(savedUser.getId());

        assertThat(beforeSaving).isFalse();
        assertThat(afterSaving).isTrue();
    }

    // proves unique = true on the join column reached the schema
    @Test
    void aSecondProfileForTheSameUser_isRejectedByTheDatabase() {
        patientRepository.saveAndFlush(buildPatient(savedUser));

        Patient duplicate = buildPatient(savedUser);
        duplicate.setBloodGroup("A+");

        assertThatThrownBy(() -> patientRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // proves nullable = false on date_of_birth reached the schema
    @Test
    void aPatientWithoutADateOfBirth_isRejected() {
        Patient patient = buildPatient(savedUser);
        patient.setDateOfBirth(null);

        assertThatThrownBy(() -> patientRepository.saveAndFlush(patient))
                .isInstanceOf(Exception.class);
    }

    // proves @Enumerated(EnumType.STRING) reached the schema.
    // a native query reads the raw column, bypassing every Hibernate conversion
    @Test
    void genderIsStoredAsTextNotAsAnOrdinal() {
        Patient saved = patientRepository.saveAndFlush(buildPatient(savedUser));

        Object rawValue = entityManager
                .createNativeQuery("select gender from patients where id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();

        assertThat(rawValue).isEqualTo("MALE");
    }
}