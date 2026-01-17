package com.ez2bg.anotherthread

import kotlin.test.*

/**
 * Unit tests for server configuration values.
 * Tests default values and constraints for service configurations.
 */
class ConfigurationTest {

    // ==================== FileUploadService Configuration ====================

    @Test
    fun fileUploadServiceAllowedExtensionsNotEmpty() {
        val extensions = FileUploadService.getAllowedExtensions()
        assertTrue(extensions.isNotEmpty(), "Allowed extensions should not be empty")
    }

    @Test
    fun fileUploadServiceAllowsCommonImageFormats() {
        assertTrue(FileUploadService.isExtensionAllowed("jpg"))
        assertTrue(FileUploadService.isExtensionAllowed("jpeg"))
        assertTrue(FileUploadService.isExtensionAllowed("png"))
        assertTrue(FileUploadService.isExtensionAllowed("gif"))
        assertTrue(FileUploadService.isExtensionAllowed("webp"))
    }

    @Test
    fun fileUploadServiceAllowsDocumentFormats() {
        assertTrue(FileUploadService.isExtensionAllowed("pdf"))
        assertTrue(FileUploadService.isExtensionAllowed("txt"))
        assertTrue(FileUploadService.isExtensionAllowed("doc"))
        assertTrue(FileUploadService.isExtensionAllowed("docx"))
    }

    @Test
    fun fileUploadServiceAllowsDataFormats() {
        assertTrue(FileUploadService.isExtensionAllowed("json"))
        assertTrue(FileUploadService.isExtensionAllowed("xml"))
        assertTrue(FileUploadService.isExtensionAllowed("csv"))
    }

    @Test
    fun fileUploadServiceAllowsArchiveFormats() {
        assertTrue(FileUploadService.isExtensionAllowed("zip"))
        assertTrue(FileUploadService.isExtensionAllowed("tar"))
        assertTrue(FileUploadService.isExtensionAllowed("gz"))
    }

    @Test
    fun fileUploadServiceRejectsExecutables() {
        assertFalse(FileUploadService.isExtensionAllowed("exe"))
        assertFalse(FileUploadService.isExtensionAllowed("sh"))
        assertFalse(FileUploadService.isExtensionAllowed("bat"))
        assertFalse(FileUploadService.isExtensionAllowed("cmd"))
    }

    @Test
    fun fileUploadServiceRejectsScriptFiles() {
        assertFalse(FileUploadService.isExtensionAllowed("js"))
        assertFalse(FileUploadService.isExtensionAllowed("py"))
        assertFalse(FileUploadService.isExtensionAllowed("rb"))
        assertFalse(FileUploadService.isExtensionAllowed("php"))
    }

    @Test
    fun fileUploadServiceExtensionCheckIsCaseInsensitive() {
        assertTrue(FileUploadService.isExtensionAllowed("PNG"))
        assertTrue(FileUploadService.isExtensionAllowed("Pdf"))
        assertTrue(FileUploadService.isExtensionAllowed("JSON"))
    }

    @Test
    fun fileUploadServiceRejectsEmptyExtension() {
        assertFalse(FileUploadService.isExtensionAllowed(""))
    }

    // ==================== Text2ImageRequest Defaults ====================

    @Test
    fun text2ImageRequestHasReasonableStepsDefault() {
        val request = Text2ImageRequest(prompt = "test")
        assertTrue(
            request.steps in 10..50,
            "Default steps (${request.steps}) should be reasonable (10-50)"
        )
    }

    @Test
    fun text2ImageRequestHasSquareImageDimensions() {
        val request = Text2ImageRequest(prompt = "test")
        assertEquals(request.width, request.height, "Default dimensions should be square")
    }

    @Test
    fun text2ImageRequestDimensionsAreMultipleOf8() {
        val request = Text2ImageRequest(prompt = "test")
        assertEquals(0, request.width % 8, "Width should be multiple of 8 for SD")
        assertEquals(0, request.height % 8, "Height should be multiple of 8 for SD")
    }

    @Test
    fun text2ImageRequestHasReasonableCfgScale() {
        val request = Text2ImageRequest(prompt = "test")
        assertTrue(
            request.cfgScale in 5.0..15.0,
            "CFG scale (${request.cfgScale}) should be reasonable (5-15)"
        )
    }

    @Test
    fun text2ImageRequestDefaultBatchSizeIsOne() {
        val request = Text2ImageRequest(prompt = "test")
        assertEquals(1, request.batchSize, "Default batch size should be 1 for efficiency")
    }

    @Test
    fun text2ImageRequestHasNegativePrompt() {
        val request = Text2ImageRequest(prompt = "test")
        assertTrue(
            request.negativePrompt.isNotBlank(),
            "Should have default negative prompt for quality"
        )
    }

    // ==================== OllamaRequest Defaults ====================

    @Test
    fun ollamaRequestDefaultStreamIsFalse() {
        val request = OllamaRequest(model = "test", prompt = "test")
        assertFalse(request.stream, "Default stream should be false for simpler handling")
    }

    @Test
    fun ollamaRequestDefaultFormatIsJson() {
        val request = OllamaRequest(model = "test", prompt = "test")
        assertEquals("json", request.format, "Default format should be json for structured output")
    }

    // ==================== GeneratedContent Tests ====================

    @Test
    fun generatedContentRequiresBothFields() {
        val content = GeneratedContent(name = "Name", description = "Desc")
        assertTrue(content.name.isNotEmpty())
        assertTrue(content.description.isNotEmpty())
    }

    @Test
    fun generatedContentAllowsEmptyStrings() {
        val content = GeneratedContent(name = "", description = "")
        assertEquals("", content.name)
        assertEquals("", content.description)
    }

    // ==================== UploadedFileInfo Tests ====================

    @Test
    fun uploadedFileInfoHasAllRequiredFields() {
        val info = UploadedFileInfo(
            filename = "test.png",
            url = "/files/uploads/test.png",
            size = 1024L,
            lastModified = System.currentTimeMillis()
        )

        assertEquals("test.png", info.filename)
        assertTrue(info.url.startsWith("/files/"))
        assertTrue(info.size > 0)
        assertTrue(info.lastModified > 0)
    }

    @Test
    fun uploadedFileInfoUrlPathFormat() {
        val info = UploadedFileInfo(
            filename = "image.jpg",
            url = "/files/uploads/image.jpg",
            size = 500L,
            lastModified = 123456789L
        )

        assertTrue(info.url.startsWith("/files/uploads/"), "URL should start with /files/uploads/")
        assertTrue(info.url.endsWith(info.filename), "URL should end with filename")
    }

    // ==================== Text2ImageResponse Tests ====================

    @Test
    fun text2ImageResponseDefaultsToNullValues() {
        val response = Text2ImageResponse()
        assertNull(response.images)
        assertNull(response.parameters)
        assertNull(response.info)
        assertNull(response.error)
        assertNull(response.detail)
    }

    @Test
    fun text2ImageResponseCanContainError() {
        val response = Text2ImageResponse(error = "Service unavailable")
        assertEquals("Service unavailable", response.error)
    }

    @Test
    fun text2ImageResponseCanContainDetail() {
        val response = Text2ImageResponse(detail = "Connection refused")
        assertEquals("Connection refused", response.detail)
    }

    // ==================== OllamaResponse Tests ====================

    @Test
    fun ollamaResponseDefaultDoneIsTrue() {
        val response = OllamaResponse(response = "test")
        assertTrue(response.done, "Default done should be true")
    }

    @Test
    fun ollamaResponseAllowsNullModel() {
        val response = OllamaResponse(model = null, response = "partial")
        assertNull(response.model)
    }
}
