package com.hyodream.backend.product.domain;

import com.hyodream.backend.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "products")
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String naverProductId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int price; // 현재 판매가

    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.ON_SALE;

    @Column(columnDefinition = "TEXT")
    private String description; // 간단 설명

    private String imageUrl; // 썸네일

    @Column(columnDefinition = "TEXT")
    private String itemUrl; // 구매 링크

    // 상세 분류 정보
    private String brand;
    private String maker;
    private String category1;
    private String category2;
    private String category3;
    private String category4;

    private String volume;
    private String sizeInfo;

    // 통계 정보 (목록용)
    private int totalSales = 0;
    private int recentSales = 0; 
    
    // 리뷰 통계 (성능 최적화를 위해 역정규화)
    private long reviewCount = 0;
    private double averageRating = 0.0;

    // AI 분석 정보 (1:1 매핑)
    @OneToOne(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ReviewAnalysis analysis;

    // 태그 정보 (유지)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_benefits", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "benefit")
    private List<String> healthBenefits = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_allergens", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "allergen")
    private List<String> allergens = new ArrayList<>();

    public void addBenefit(String benefit) {
        this.healthBenefits.add(benefit);
    }

    public void addAllergen(String allergen) {
        this.allergens.add(allergen);
    }
}
