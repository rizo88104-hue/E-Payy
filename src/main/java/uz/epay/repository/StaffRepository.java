package uz.epay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uz.epay.entity.Staff;
import uz.epay.entity.StaffRole;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    Optional<Staff> findByUsername(String username);

    boolean existsByUsername(String username);

    List<Staff> findAllByOrderByRoleAscFullNameAsc();

    long countByRoleAndActiveTrue(StaffRole role);
}
