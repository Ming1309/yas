package com.yas.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.media.config.YasConfig;
import com.yas.media.mapper.MediaVmMapper;
import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.model.dto.MediaDto.MediaDtoBuilder;
import com.yas.media.repository.FileSystemRepository;
import com.yas.media.repository.MediaRepository;
import com.yas.media.service.MediaServiceImpl;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import com.yas.media.viewmodel.NoFileMediaVm;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@DisplayName("MediaService Unit Tests")
class MediaServiceUnitTest {

    @Spy
    private MediaVmMapper mediaVmMapper = Mappers.getMapper(MediaVmMapper.class);

    @Mock
    private MediaRepository mediaRepository;

    @Mock
    private FileSystemRepository fileSystemRepository;

    @Mock
    private YasConfig yasConfig;

    @InjectMocks
    private MediaServiceImpl mediaService;

    private Media media;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        media = new Media();
        media.setId(1L);
        media.setCaption("test");
        media.setFileName("file");
        media.setMediaType("image/jpeg");
        media.setFilePath("/path/to/file");
    }

    @Nested
    @DisplayName("saveMedia Tests")
    class SaveMediaTests {

        @Test
        @DisplayName("Should save media with PNG type successfully")
        void saveMedia_whenTypePNG_thenSaveSuccess() {
            byte[] pngFileContent = new byte[] {};
            MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "example.png",
                "image/png",
                pngFileContent
            );
            MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "fileName");

            when(fileSystemRepository.persistFile(anyString(), any(byte[].class)))
                .thenReturn("/path/to/file");
            when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertNotNull(mediaSave);
            assertEquals("media", mediaSave.getCaption());
            assertEquals("fileName", mediaSave.getFileName());
            assertEquals("image/png", mediaSave.getMediaType());
        }

        @Test
        @DisplayName("Should save media with JPEG type successfully")
        void saveMedia_whenTypeJPEG_thenSaveSuccess() {
            byte[] jpegFileContent = new byte[] {};
            MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "example.jpeg",
                "image/jpeg",
                jpegFileContent
            );
            MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "fileName");

            when(fileSystemRepository.persistFile(anyString(), any(byte[].class)))
                .thenReturn("/path/to/file");
            when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertNotNull(mediaSave);
            assertEquals("media", mediaSave.getCaption());
            assertEquals("fileName", mediaSave.getFileName());
            assertEquals("image/jpeg", mediaSave.getMediaType());
        }

        @Test
        @DisplayName("Should save media with GIF type successfully")
        void saveMedia_whenTypeGIF_thenSaveSuccess() {
            byte[] gifFileContent = new byte[] {};
            MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "example.gif",
                "image/gif",
                gifFileContent
            );
            MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "fileName");

            when(fileSystemRepository.persistFile(anyString(), any(byte[].class)))
                .thenReturn("/path/to/file");
            when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertNotNull(mediaSave);
            assertEquals("media", mediaSave.getCaption());
            assertEquals("fileName", mediaSave.getFileName());
        }

        @Test
        @DisplayName("Should use original filename when override is null")
        void saveMedia_whenFileNameIsNull_thenUseOriginalFileName() {
            byte[] pngFileContent = new byte[] {};
            MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "example.png",
                "image/png",
                pngFileContent
            );
            MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, null);

            when(fileSystemRepository.persistFile(anyString(), any(byte[].class)))
                .thenReturn("/path/to/file");
            when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertNotNull(mediaSave);
            assertEquals("example.png", mediaSave.getFileName());
        }

        @Test
        @DisplayName("Should use original filename when override is empty")
        void saveMedia_whenFileNameIsEmpty_thenUseOriginalFileName() {
            byte[] pngFileContent = new byte[] {};
            MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "example.png",
                "image/png",
                pngFileContent
            );
            MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "");

            when(fileSystemRepository.persistFile(anyString(), any(byte[].class)))
                .thenReturn("/path/to/file");
            when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertNotNull(mediaSave);
            assertEquals("example.png", mediaSave.getFileName());
        }

        @Test
        @DisplayName("Should use original filename when override is blank")
        void saveMedia_whenFileNameIsBlank_thenUseOriginalFileName() {
            byte[] pngFileContent = new byte[] {};
            MultipartFile multipartFile = new MockMultipartFile(
                "file",
                "example.png",
                "image/png",
                pngFileContent
            );
            MediaPostVm mediaPostVm = new MediaPostVm("media", multipartFile, "   ");

            when(fileSystemRepository.persistFile(anyString(), any(byte[].class)))
                .thenReturn("/path/to/file");
            when(mediaRepository.save(any(Media.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Media mediaSave = mediaService.saveMedia(mediaPostVm);

            assertNotNull(mediaSave);
            assertEquals("example.png", mediaSave.getFileName());
        }
    }

    @Nested
    @DisplayName("getMediaById Tests")
    class GetMediaByIdTests {

        @Test
        @DisplayName("Should return MediaVm when valid ID is provided")
        void getMedia_whenValidId_thenReturnData() {
            NoFileMediaVm noFileMediaVm = new NoFileMediaVm(1L, "Test", "fileName", "image/png");
            when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(noFileMediaVm);
            when(yasConfig.publicUrl()).thenReturn("/media/");

            MediaVm mediaVm = mediaService.getMediaById(1L);

            assertNotNull(mediaVm);
            assertEquals("Test", mediaVm.getCaption());
            assertEquals("fileName", mediaVm.getFileName());
            assertEquals("image/png", mediaVm.getMediaType());
            assertEquals("/media/medias/1/file/fileName", mediaVm.getUrl());
        }

        @Test
        @DisplayName("Should return null when media is not found")
        void getMedia_whenMediaNotFound_thenReturnNull() {
            when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(null);

            MediaVm mediaVm = mediaService.getMediaById(1L);

            assertNull(mediaVm);
        }

        @Test
        @DisplayName("Should construct correct URL with public base URL")
        void getMedia_whenValidId_thenURLConstructedCorrectly() {
            NoFileMediaVm noFileMediaVm = new NoFileMediaVm(5L, "Caption", "test.jpg", "image/jpeg");
            when(mediaRepository.findByIdWithoutFileInReturn(5L)).thenReturn(noFileMediaVm);
            when(yasConfig.publicUrl()).thenReturn("https://example.com/api/");

            MediaVm mediaVm = mediaService.getMediaById(5L);

            assertNotNull(mediaVm);
            assertTrue(mediaVm.getUrl().contains("/medias/5/file/test.jpg"));
        }
    }

    @Nested
    @DisplayName("removeMedia Tests")
    class RemoveMediaTests {

        @Test
        @DisplayName("Should throw NotFoundException when media not found")
        void removeMedia_whenMediaNotFound_thenThrowsNotFoundException() {
            when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(null);

            NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> mediaService.removeMedia(1L)
            );

            assertEquals("Media 1 is not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should delete media successfully when valid ID")
        void removeMedia_whenValidId_thenRemoveSuccess() {
            NoFileMediaVm noFileMediaVm = new NoFileMediaVm(1L, "Test", "fileName", "image/png");
            when(mediaRepository.findByIdWithoutFileInReturn(1L)).thenReturn(noFileMediaVm);
            doNothing().when(mediaRepository).deleteById(1L);

            mediaService.removeMedia(1L);

            verify(mediaRepository, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("Should call repository delete exactly once")
        void removeMedia_whenValidId_thenRepositoryDeleteCalledOnce() {
            NoFileMediaVm noFileMediaVm = new NoFileMediaVm(2L, "Test", "fileName", "image/png");
            when(mediaRepository.findByIdWithoutFileInReturn(2L)).thenReturn(noFileMediaVm);
            doNothing().when(mediaRepository).deleteById(2L);

            mediaService.removeMedia(2L);

            verify(mediaRepository, times(1)).deleteById(2L);
            verify(mediaRepository, times(1)).findByIdWithoutFileInReturn(2L);
        }
    }

    @Nested
    @DisplayName("getFile Tests")
    class GetFileTests {

        @Test
        @DisplayName("Should return empty MediaDto when media not found")
        void getFile_whenMediaNotFound_thenReturnEmptyMediaDto() {
            when(mediaRepository.findById(1L)).thenReturn(Optional.empty());

            MediaDto mediaDto = mediaService.getFile(1L, "fileName");

            assertNotNull(mediaDto);
            assertNull(mediaDto.getContent());
            assertNull(mediaDto.getMediaType());
        }

        @Test
        @DisplayName("Should return empty MediaDto when filename doesn't match (case-insensitive)")
        void getFile_whenMediaNameNotMatch_thenReturnEmptyMediaDto() {
            when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));

            MediaDto mediaDto = mediaService.getFile(1L, "different-file");

            assertNotNull(mediaDto);
            assertNull(mediaDto.getContent());
            assertNull(mediaDto.getMediaType());
        }

        @Test
        @DisplayName("Should return MediaDto with correct content when filename matches")
        void getFile_whenValidIdAndFileName_thenReturnMediaDto() {
            InputStream fileStream = new ByteArrayInputStream("file content".getBytes());
            when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
            when(fileSystemRepository.getFile("/path/to/file")).thenReturn(fileStream);

            MediaDto mediaDto = mediaService.getFile(1L, "file");

            assertNotNull(mediaDto);
            assertNotNull(mediaDto.getContent());
            assertEquals(org.springframework.http.MediaType.IMAGE_JPEG, mediaDto.getMediaType());
        }

        @Test
        @DisplayName("Should match filename case-insensitively")
        void getFile_whenFileNameCaseMismatch_thenMatch() {
            InputStream fileStream = new ByteArrayInputStream("file content".getBytes());
            when(mediaRepository.findById(1L)).thenReturn(Optional.of(media));
            when(fileSystemRepository.getFile("/path/to/file")).thenReturn(fileStream);

            MediaDto mediaDto = mediaService.getFile(1L, "FILE");

            assertNotNull(mediaDto);
            assertNotNull(mediaDto.getContent());
        }
    }

    @Nested
    @DisplayName("getMediaByIds Tests")
    class GetMediaByIdsTests {

        @Test
        @DisplayName("Should return list of MediaVm for valid IDs")
        void getMediaByIds_whenValidIds_thenReturnMediaList() {
            Media media1 = getMedia(1L, "iPhone 15");
            Media media2 = getMedia(2L, "MacBook");
            List<Media> mediaList = Arrays.asList(media1, media2);

            when(mediaRepository.findAllById(Arrays.asList(1L, 2L))).thenReturn(mediaList);
            when(yasConfig.publicUrl()).thenReturn("https://media.example.com/");

            List<MediaVm> result = mediaService.getMediaByIds(Arrays.asList(1L, 2L));

            assertFalse(result.isEmpty());
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(m -> m.getUrl() != null));
        }

        @Test
        @DisplayName("Should return empty list when no media found")
        void getMediaByIds_whenNoMediaFound_thenReturnEmptyList() {
            when(mediaRepository.findAllById(Collections.emptyList())).thenReturn(Collections.emptyList());

            List<MediaVm> result = mediaService.getMediaByIds(Collections.emptyList());

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should construct correct URLs for all medias")
        void getMediaByIds_whenValidIds_thenURLsConstructedCorrectly() {
            Media media1 = getMedia(1L, "test1.jpg");
            Media media2 = getMedia(2L, "test2.png");
            List<Media> mediaList = Arrays.asList(media1, media2);

            when(mediaRepository.findAllById(Arrays.asList(1L, 2L))).thenReturn(mediaList);
            when(yasConfig.publicUrl()).thenReturn("https://api.example.com/");

            List<MediaVm> result = mediaService.getMediaByIds(Arrays.asList(1L, 2L));

            assertEquals(2, result.size());
            result.forEach(vm -> {
                assertNotNull(vm.getUrl());
                assertTrue(vm.getUrl().contains("/medias/"));
                assertTrue(vm.getUrl().contains("/file/"));
            });
        }

        @Test
        @DisplayName("Should verify mapper was called for each media")
        void getMediaByIds_whenValidIds_thenMapperCalled() {
            Media media1 = getMedia(1L, "file1.jpg");
            Media media2 = getMedia(2L, "file2.png");
            List<Media> mediaList = Arrays.asList(media1, media2);

            when(mediaRepository.findAllById(Arrays.asList(1L, 2L))).thenReturn(mediaList);
            when(yasConfig.publicUrl()).thenReturn("https://media.example.com/");

            mediaService.getMediaByIds(Arrays.asList(1L, 2L));

            verify(mediaVmMapper, times(2)).toVm(any(Media.class));
        }
    }

    private static Media getMedia(Long id, String fileName) {
        Media m = new Media();
        m.setId(id);
        m.setFileName(fileName);
        m.setCaption("Test " + id);
        m.setMediaType("image/jpeg");
        m.setFilePath("/path/" + id);
        return m;
    }
}
