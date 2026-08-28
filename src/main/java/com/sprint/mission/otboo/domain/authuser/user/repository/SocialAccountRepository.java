package com.sprint.mission.otboo.domain.authuser.user.repository;

import com.sprint.mission.otboo.domain.authuser.user.entity.SocialAccount;
import com.sprint.mission.otboo.domain.authuser.user.entity.enums.OAuth2Provider;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

  @Query("select sa from SocialAccount sa join fetch sa.user "
      + "where sa.provider = :provider and sa.providerId = :providerId")
  Optional<SocialAccount> findByProviderAndProviderId(@Param("provider") OAuth2Provider provider,
      @Param("providerId") String providerId);

  @Query("select sa.provider from SocialAccount sa where sa.user.id = :userId")
  List<OAuth2Provider> findLinkedProvidersByUserId(@Param("userId") UUID userId);
}
