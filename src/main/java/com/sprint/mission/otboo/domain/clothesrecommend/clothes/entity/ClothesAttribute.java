package com.sprint.mission.otboo.domain.clothesrecommend.clothes.entity;

import com.sprint.mission.otboo.domain.clothesrecommend.attributedef.entity.ClothesAttributeDef;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(
    name = "clothes_attributes",
    uniqueConstraints = @UniqueConstraint(
        name = "UQ_clothes_attributes_clothes_id_definition_id",
        columnNames = {"clothes_id", "definition_id"}))
@Entity
public class ClothesAttribute {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", nullable = false, updatable = false)
  private UUID id;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  private Instant updatedAt;

  @Column(name = "clothes_id", nullable = false)
  private UUID clothesId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "definition_id",
      nullable = false,
      foreignKey = @ForeignKey(
          name = "FK_clothes_attribute_defs_TO_clothes_attributes_1"))
  private ClothesAttributeDef definition;

  @Column(nullable = false, length = 50)
  private String value;

  public static ClothesAttribute create(
      UUID clothesId, ClothesAttributeDef definition, String value) {
    ClothesAttribute attribute = new ClothesAttribute();
    attribute.clothesId = clothesId;
    attribute.definition = definition;
    attribute.value = value;
    return attribute;
  }
}