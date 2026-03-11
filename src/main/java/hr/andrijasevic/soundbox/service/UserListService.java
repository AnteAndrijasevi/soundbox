package hr.andrijasevic.soundbox.service;

import hr.andrijasevic.soundbox.domain.Album;
import hr.andrijasevic.soundbox.domain.ListItem;
import hr.andrijasevic.soundbox.domain.User;
import hr.andrijasevic.soundbox.domain.UserList;
import hr.andrijasevic.soundbox.dto.CreateListRequest;
import hr.andrijasevic.soundbox.dto.ListItemDto;
import hr.andrijasevic.soundbox.dto.UserListDetailDto;
import hr.andrijasevic.soundbox.dto.UserListDto;
import hr.andrijasevic.soundbox.repository.AlbumRepository;
import hr.andrijasevic.soundbox.repository.ListItemRepository;
import hr.andrijasevic.soundbox.repository.UserListRepository;
import hr.andrijasevic.soundbox.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserListService {

    private final UserListRepository userListRepository;
    private final ListItemRepository listItemRepository;
    private final UserRepository userRepository;
    private final AlbumRepository albumRepository;
    private final AlbumService albumService;

    public UserListService(
            UserListRepository userListRepository,
            ListItemRepository listItemRepository,
            UserRepository userRepository,
            AlbumRepository albumRepository,
            AlbumService albumService
    ) {
        this.userListRepository = userListRepository;
        this.listItemRepository = listItemRepository;
        this.userRepository = userRepository;
        this.albumRepository = albumRepository;
        this.albumService = albumService;
    }

    public UserListDto createList(CreateListRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserList userList = new UserList();
        userList.setUser(user);
        userList.setName(request.name());
        userList.setDescription(request.description());
        userList.setPublic(request.isPublic());
        userList.setCreatedAt(LocalDateTime.now());

        UserList saved = userListRepository.save(userList);
        return mapToDto(saved);
    }

    public List<UserListDto> getLists(Long userId) {
        return userListRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToDto)
                .toList();
    }

    public UserListDetailDto getListDetail(Long listId) {
        UserList list = userListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));
        List<ListItem> items = listItemRepository.findByUserListIdOrderByPosition(listId);
        return mapToDetailDto(list, items);
    }

    public UserListDetailDto addAlbumToList(Long listId, String mbid, String note, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserList list = userListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));

        if (!list.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized");
        }

        Album album = albumRepository.findByMbid(mbid).orElseGet(() -> {
            albumService.getAlbum(mbid);
            return albumRepository.findByMbid(mbid).orElseThrow();
        });

        int position = listItemRepository.countByUserListId(listId) + 1;

        ListItem listItem = new ListItem();
        listItem.setUserList(list);
        listItem.setAlbum(album);
        listItem.setPosition(position);
        listItem.setNote(note);

        listItemRepository.save(listItem);
        return getListDetail(listId);
    }

    public void removeAlbumFromList(Long listId, String mbid, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserList list = userListRepository.findById(listId)
                .orElseThrow();

        if (!list.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized");
        }

        Album album = albumRepository.findByMbid(mbid)
                .orElseThrow(() -> new RuntimeException("Album not found"));

        listItemRepository.deleteByUserListIdAndAlbumId(listId, album.getId());
    }

    public void deleteList(Long listId, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserList list = userListRepository.findById(listId)
                .orElseThrow();

        if (!list.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Not authorized");
        }

        userListRepository.delete(list);
    }

    private UserListDto mapToDto(UserList list) {
        int itemCount = listItemRepository.countByUserListId(list.getId());
        String username = list.getUser() != null ? list.getUser().getDisplayUsername() : null;
        return new UserListDto(
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.isPublic(),
                username,
                itemCount,
                list.getCreatedAt()
        );
    }

    private UserListDetailDto mapToDetailDto(UserList list, List<ListItem> items) {
        String username = list.getUser() != null ? list.getUser().getDisplayUsername() : null;
        List<ListItemDto> itemDtos = items.stream().map(item -> {
            Album album = item.getAlbum();
            return new ListItemDto(
                    album != null ? album.getMbid() : null,
                    album != null ? album.getTitle() : null,
                    album != null ? album.getCoverArtUrl() : null,
                    album != null && album.getArtist() != null ? album.getArtist().getName() : null,
                    item.getPosition(),
                    item.getNote()
            );
        }).toList();
        return new UserListDetailDto(
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.isPublic(),
                username,
                itemDtos,
                list.getCreatedAt()
        );
    }
}
