package repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shareplate.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}