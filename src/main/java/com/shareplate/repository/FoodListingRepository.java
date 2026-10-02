package com.shareplate.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.shareplate.entity.*;
public interface FoodListingRepository extends JpaRepository<FoodListing,Long>{
 List<FoodListing> findByStatus(ListingStatus status); List<FoodListing> findByDonorId(Long donorId);
}