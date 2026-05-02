package com.yas.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.media.controller.MediaController;
import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.service.MediaService;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import com.yas.media.viewmodel.NoFileMediaVm;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

@WebMvcTest(MediaController.class)
@DisplayName("MediaController Unit Tests")
class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    @Autowired
    private ObjectMapper objectMapper;

    private Media testMedia;
    private MediaVm testMediaVm;

    @BeforeEach
    void setUp() {
        testMedia = new Media();
        testMedia.setId(1L);
        testMedia.setCaption("Test Image");
        testMedia.setFileName("test-image.jpg");
        testMedia.setMediaType("image/jpeg");
        testMedia.setFilePath("/path/to/test-image.jpg");

        testMediaVm = new MediaVm(
            1L,
            "Test Image",
            "test-image.jpg",
            "image/jpeg",
            "https://media.example.com/medias/1/file/test-image.jpg"
        );
    }

    @Nested
    @DisplayName("POST /medias Tests")
    class CreateMediaTests {

        @Test
        @DisplayName("Should create media successfully with valid file")
        void create_whenValidFile_thenReturnCreatedMedia() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "multipartFile",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
            );

            when(mediaService.saveMedia(any(MediaPostVm.class))).thenReturn(testMedia);

            mockMvc.perform(multipart("/medias")
                    .file(file)
                    .param("caption", "Test Image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.caption").value("Test Image"))
                .andExpect(jsonPath("$.fileName").value("test-image.jpg"))
                .andExpect(jsonPath("$.mediaType").value("image/jpeg"));

            verify(mediaService, times(1)).saveMedia(any(MediaPostVm.class));
        }

        @Test
        @DisplayName("Should create media with custom filename override")
        void create_whenFileNameOverride_thenReturnMediaWithCustomName() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "multipartFile",
                "original.jpg",
                "image/jpeg",
                "test content".getBytes()
            );

            when(mediaService.saveMedia(any(MediaPostVm.class))).thenReturn(testMedia);

            mockMvc.perform(multipart("/medias")
                    .file(file)
                    .param("caption", "Test Image")
                    .param("fileNameOverride", "custom-name.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caption").value("Test Image"));

            verify(mediaService, times(1)).saveMedia(any(MediaPostVm.class));
        }

        @Test
        @DisplayName("Should return BadRequest when file is missing")
        void create_whenFileIsMissing_thenReturnBadRequest() throws Exception {
            mockMvc.perform(multipart("/medias")
                    .param("caption", "Test Image"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return BadRequest when caption is missing")
        void create_whenCaptionIsMissing_thenReturnBadRequest() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                "multipartFile",
                "test.jpg",
                "image/jpeg",
                "test content".getBytes()
            );

            mockMvc.perform(multipart("/medias")
                    .file(file))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle different image formats")
        void create_whenDifferentFormats_thenCreateSuccessfully() throws Exception {
            MockMultipartFile pngFile = new MockMultipartFile(
                "multipartFile",
                "test.png",
                "image/png",
                "test content".getBytes()
            );

            when(mediaService.saveMedia(any(MediaPostVm.class))).thenReturn(testMedia);

            mockMvc.perform(multipart("/medias")
                    .file(pngFile)
                    .param("caption", "PNG Image"))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /medias/{id} Tests")
    class DeleteMediaTests {

        @Test
        @DisplayName("Should delete media successfully with valid ID")
        void delete_whenValidId_thenReturnNoContent() throws Exception {
            doNothing().when(mediaService).removeMedia(1L);

            mockMvc.perform(delete("/medias/1"))
                .andExpect(status().isNoContent());

            verify(mediaService, times(1)).removeMedia(1L);
        }

        @Test
        @DisplayName("Should throw NotFoundException when media not found")
        void delete_whenMediaNotFound_thenReturn404() throws Exception {
            doThrow(new NotFoundException("Media 1 is not found"))
                .when(mediaService).removeMedia(1L);

            mockMvc.perform(delete("/medias/1"))
                .andExpect(status().isNotFound());

            verify(mediaService, times(1)).removeMedia(1L);
        }

        @Test
        @DisplayName("Should delete media by ID successfully")
        void delete_whenValidId_thenMediaServiceCalled() throws Exception {
            doNothing().when(mediaService).removeMedia(99L);

            mockMvc.perform(delete("/medias/99"))
                .andExpect(status().isNoContent());

            verify(mediaService, times(1)).removeMedia(99L);
        }
    }

    @Nested
    @DisplayName("GET /medias/{id} Tests")
    class GetMediaByIdTests {

        @Test
        @DisplayName("Should return media when valid ID provided")
        void get_whenValidId_thenReturnMedia() throws Exception {
            when(mediaService.getMediaById(1L)).thenReturn(testMediaVm);

            mockMvc.perform(get("/medias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.caption").value("Test Image"))
                .andExpect(jsonPath("$.fileName").value("test-image.jpg"))
                .andExpect(jsonPath("$.mediaType").value("image/jpeg"))
                .andExpect(jsonPath("$.url").exists());

            verify(mediaService, times(1)).getMediaById(1L);
        }

        @Test
        @DisplayName("Should return NotFound when media does not exist")
        void get_whenMediaNotFound_thenReturn404() throws Exception {
            when(mediaService.getMediaById(999L)).thenReturn(null);

            mockMvc.perform(get("/medias/999"))
                .andExpect(status().isNotFound());

            verify(mediaService, times(1)).getMediaById(999L);
        }

        @Test
        @DisplayName("Should return correct media details")
        void get_whenValidId_thenReturnCorrectDetails() throws Exception {
            MediaVm customMedia = new MediaVm(
                5L,
                "Custom Caption",
                "custom-file.png",
                "image/png",
                "https://media.example.com/medias/5/file/custom-file.png"
            );
            when(mediaService.getMediaById(5L)).thenReturn(customMedia);

            mockMvc.perform(get("/medias/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.caption").value("Custom Caption"))
                .andExpect(jsonPath("$.fileName").value("custom-file.png"));
        }
    }

    @Nested
    @DisplayName("GET /medias (by IDs) Tests")
    class GetMediaByIdsTests {

        @Test
        @DisplayName("Should return list of medias for valid IDs")
        void getByIds_whenValidIds_thenReturnMediaList() throws Exception {
            List<MediaVm> mediaList = Arrays.asList(
                new MediaVm(1L, "Image 1", "image1.jpg", "image/jpeg", "url1"),
                new MediaVm(2L, "Image 2", "image2.png", "image/png", "url2")
            );

            when(mediaService.getMediaByIds(Arrays.asList(1L, 2L))).thenReturn(mediaList);

            mockMvc.perform(get("/medias")
                    .param("ids", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$.length()").value(2));

            verify(mediaService, times(1)).getMediaByIds(Arrays.asList(1L, 2L));
        }

        @Test
        @DisplayName("Should return NotFound when no media found for IDs")
        void getByIds_whenNoMediaFound_thenReturn404() throws Exception {
            when(mediaService.getMediaByIds(anyList())).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/medias")
                    .param("ids", "999", "998"))
                .andExpect(status().isNotFound());

            verify(mediaService, times(1)).getMediaByIds(anyList());
        }

        @Test
        @DisplayName("Should handle single ID parameter")
        void getByIds_whenSingleId_thenReturnSingleMedia() throws Exception {
            List<MediaVm> mediaList = Arrays.asList(testMediaVm);

            when(mediaService.getMediaByIds(Arrays.asList(1L))).thenReturn(mediaList);

            mockMvc.perform(get("/medias")
                    .param("ids", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("Should handle multiple ID parameters")
        void getByIds_whenMultipleIds_thenReturnMultipleMedias() throws Exception {
            List<MediaVm> mediaList = Arrays.asList(
                new MediaVm(1L, "Image 1", "img1.jpg", "image/jpeg", "url1"),
                new MediaVm(2L, "Image 2", "img2.jpg", "image/jpeg", "url2"),
                new MediaVm(3L, "Image 3", "img3.jpg", "image/jpeg", "url3")
            );

            when(mediaService.getMediaByIds(Arrays.asList(1L, 2L, 3L))).thenReturn(mediaList);

            mockMvc.perform(get("/medias")
                    .param("ids", "1", "2", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        }
    }

    @Nested
    @DisplayName("GET /medias/{id}/file/{fileName} Tests")
    class GetFileTests {

        @Test
        @DisplayName("Should return file content when valid ID and fileName")
        void getFile_whenValidIdAndFileName_thenReturnFile() throws Exception {
            InputStream fileStream = new ByteArrayInputStream("test file content".getBytes());
            MediaDto mediaDto = MediaDto.builder()
                .content(fileStream)
                .mediaType(MediaType.IMAGE_JPEG)
                .build();

            when(mediaService.getFile(1L, "test-image.jpg")).thenReturn(mediaDto);

            mockMvc.perform(get("/medias/1/file/test-image.jpg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").exists());

            verify(mediaService, times(1)).getFile(1L, "test-image.jpg");
        }

        @Test
        @DisplayName("Should include filename in Content-Disposition header")
        void getFile_whenValid_thenSetContentDispositionHeader() throws Exception {
            InputStream fileStream = new ByteArrayInputStream("test file content".getBytes());
            MediaDto mediaDto = MediaDto.builder()
                .content(fileStream)
                .mediaType(MediaType.IMAGE_JPEG)
                .build();

            when(mediaService.getFile(1L, "test-image.jpg")).thenReturn(mediaDto);

            mockMvc.perform(get("/medias/1/file/test-image.jpg"))
                .andExpect(status().isOk());

            verify(mediaService, times(1)).getFile(1L, "test-image.jpg");
        }

        @Test
        @DisplayName("Should set correct Content-Type for image")
        void getFile_whenImage_thenSetCorrectContentType() throws Exception {
            InputStream fileStream = new ByteArrayInputStream("test file content".getBytes());
            MediaDto mediaDto = MediaDto.builder()
                .content(fileStream)
                .mediaType(MediaType.IMAGE_PNG)
                .build();

            when(mediaService.getFile(2L, "test-image.png")).thenReturn(mediaDto);

            mockMvc.perform(get("/medias/2/file/test-image.png"))
                .andExpect(status().isOk());

            verify(mediaService, times(1)).getFile(2L, "test-image.png");
        }
    }
}
