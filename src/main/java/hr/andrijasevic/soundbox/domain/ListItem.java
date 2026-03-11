package hr.andrijasevic.soundbox.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "list_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id", nullable = false)
    private UserList userList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = false)
    private Album album;

    private Integer position;

    private String note;
}
