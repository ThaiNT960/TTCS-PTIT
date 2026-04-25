package com.example.ptitsocialchat.entity;

import com.example.ptitsocialchat.enums.PrivacySetting;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "groups_table") // 'groups' is a reserved keyword in some SQL dialects
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String coverPhoto;

    @Enumerated(EnumType.STRING)
    private PrivacySetting privacy = PrivacySetting.PUBLIC;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> members;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverPhoto() { return coverPhoto; }
    public void setCoverPhoto(String coverPhoto) { this.coverPhoto = coverPhoto; }
    public PrivacySetting getPrivacy() { return privacy; }
    public void setPrivacy(PrivacySetting privacy) { this.privacy = privacy; }
    public List<GroupMember> getMembers() { return members; }
    public void setMembers(List<GroupMember> members) { this.members = members; }
}
