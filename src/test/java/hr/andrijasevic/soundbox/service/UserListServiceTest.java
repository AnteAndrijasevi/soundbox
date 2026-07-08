package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.Album;
import hr.andrijasevic.soundbox.domain.ListItem;
import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.domain.UserList;
import hr.andrijasevic.soundbox.dto.CreateListRequest;
import hr.andrijasevic.soundbox.dto.UserListDetailDto;
import hr.andrijasevic.soundbox.dto.UserListDto;
import hr.andrijasevic.soundbox.repository.AlbumRepository;
import hr.andrijasevic.soundbox.repository.ListItemRepository;
import hr.andrijasevic.soundbox.repository.UserListRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserListServiceTest {

    private static final String MBID = "11111111-1111-1111-1111-111111111111";

    @Mock
    private UserListRepository userListRepository;
    @Mock
    private ListItemRepository listItemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AlbumRepository albumRepository;
    @Mock
    private AlbumService albumService;

    @InjectMocks
    private UserListService userListService;

    private User owner;
    private User stranger;
    private UserList list;
    private Album album;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("ante").email("ante@example.com").passwordHash("x").build();
        stranger = User.builder().id(2L).username("vera").email("vera@example.com").passwordHash("x").build();
        list = new UserList();
        list.setId(5L);
        list.setUser(owner);
        list.setName("Albums that raised me");
        list.setPublic(true);
        album = Album.builder().id(10L).mbid(MBID).title("OK Computer")
                .artworkUrl("https://itunes.example/600x600.jpg").build();
    }

    @Test
    void createList_savesAndMapsList() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(owner));
        when(userListRepository.save(any(UserList.class))).thenAnswer(inv -> {
            UserList l = inv.getArgument(0);
            l.setId(5L);
            return l;
        });

        UserListDto dto = userListService.createList(
                new CreateListRequest("Albums that raised me", "The formative shelf.", true), "ante@example.com");

        assertThat(dto.name()).isEqualTo("Albums that raised me");
        assertThat(dto.username()).isEqualTo("ante");
        assertThat(dto.isPublic()).isTrue();
        assertThat(dto.itemCount()).isZero();
    }

    @Test
    void addAlbumToList_appendsAtNextPositionAndReturnsDetail() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(owner));
        when(userListRepository.findById(5L)).thenReturn(Optional.of(list));
        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.of(album));
        when(listItemRepository.countByUserListId(5L)).thenReturn(2);
        ListItem existing = new ListItem();
        existing.setAlbum(album);
        existing.setPosition(3);
        when(listItemRepository.findByUserListIdOrderByPosition(5L)).thenReturn(List.of(existing));

        UserListDetailDto detail = userListService.addAlbumToList(5L, MBID, "a note", "ante@example.com");

        ArgumentCaptor<ListItem> captor = ArgumentCaptor.forClass(ListItem.class);
        verify(listItemRepository).save(captor.capture());
        assertThat(captor.getValue().getPosition()).isEqualTo(3); // count 2 → appended at 3
        assertThat(captor.getValue().getNote()).isEqualTo("a note");
        // detail mapping prefers iTunes artwork
        assertThat(detail.items().get(0).coverArtUrl()).isEqualTo("https://itunes.example/600x600.jpg");
    }

    @Test
    void addAlbumToList_rejectsNonOwner() {
        when(userRepository.findByEmail("vera@example.com")).thenReturn(Optional.of(stranger));
        when(userListRepository.findById(5L)).thenReturn(Optional.of(list));

        assertThatThrownBy(() -> userListService.addAlbumToList(5L, MBID, null, "vera@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not authorized");

        verify(listItemRepository, never()).save(any());
    }

    @Test
    void removeAlbumFromList_deletesItemForOwner() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(owner));
        when(userListRepository.findById(5L)).thenReturn(Optional.of(list));
        when(albumRepository.findByMbid(MBID)).thenReturn(Optional.of(album));

        userListService.removeAlbumFromList(5L, MBID, "ante@example.com");

        verify(listItemRepository).deleteByUserListIdAndAlbumId(5L, 10L);
    }

    @Test
    void deleteList_rejectsNonOwner() {
        when(userRepository.findByEmail("vera@example.com")).thenReturn(Optional.of(stranger));
        when(userListRepository.findById(5L)).thenReturn(Optional.of(list));

        assertThatThrownBy(() -> userListService.deleteList(5L, "vera@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not authorized");

        verify(userListRepository, never()).delete(any());
    }

    @Test
    void deleteList_deletesForOwner() {
        when(userRepository.findByEmail("ante@example.com")).thenReturn(Optional.of(owner));
        when(userListRepository.findById(5L)).thenReturn(Optional.of(list));

        userListService.deleteList(5L, "ante@example.com");

        verify(userListRepository).delete(list);
    }
}
