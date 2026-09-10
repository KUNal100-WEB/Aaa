package com.example.domain.validation

object PathValidator {
    private val ALLOWED_EXTENSIONS = listOf(
        ".kt", ".kts", ".xml", ".json", ".gradle", ".properties", ".md", ".txt"
    )

    data class ValidationResult(val isValid: Boolean, val errorMessage: String? = null)

    fun validate(path: String): ValidationResult {
        if (path.isBlank()) {
            return ValidationResult(false, "File path cannot be blank.")
        }
        if (path.contains("..") || path.startsWith("/") || path.startsWith("\\")) {
            return ValidationResult(false, "Path traversal or root-relative paths are strictly prohibited.")
        }
        val lower = path.lowercase()
        val hasAllowedExtension = ALLOWED_EXTENSIONS.any { lower.endsWith(it) }
        if (!hasAllowedExtension) {
            return ValidationResult(
                false,
                "Unsupported file format. Permitted types: ${ALLOWED_EXTENSIONS.joinToString(", ")}"
            )
        }
        return ValidationResult(true)
    }
}
