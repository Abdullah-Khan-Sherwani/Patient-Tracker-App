package com.example.patienttracker.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.patienttracker.data.HealthRecord
import java.text.SimpleDateFormat
import java.util.*

// ============================================================
// Deep Teal Design System - Health Records
// ============================================================
private val HeaderTopColor = Color(0xFF0E4944)      // Deep Teal
private val AccentColor = Color(0xFF0F8B8D)         // Teal accent
private val SurfaceColor = Color(0xFFFFFFFF)        // White
private val BackgroundColor = Color(0xFFF0F5F4)     // Light teal-tinted
private val TextPrimary = Color(0xFF1F2937)         // Dark text
private val TextSecondary = Color(0xFF6B7280)       // Gray text
private val PrivateColor = Color(0xFFDC2626)        // Red for private
private val PdfColor = Color(0xFFE53935)            // PDF red
private val DividerColor = Color(0xFFE5E7EB)        // Light gray

/**
 * Compact, clean health record card for list view
 * Shows essential information only - details on click
 */
@Composable
fun CompactRecordCard(
    record: HealthRecord,
    context: Context,
    onOpenFile: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    showGlassBreakButton: Boolean = false,
    onGlassBreak: (() -> Unit)? = null
) {
    // Check if this is a locked private record
    val isLockedPrivate = showGlassBreakButton && record.isPrivate
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = HeaderTopColor.copy(alpha = 0.08f),
                spotColor = HeaderTopColor.copy(alpha = 0.08f)
            )
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isLockedPrivate) PrivateColor.copy(alpha = 0.05f) else SurfaceColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail (64x64)
            if (isLockedPrivate) {
                // Show lock icon for inaccessible private records
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrivateColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Private",
                        tint = PrivateColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                RecordThumbnail(
                    record = record,
                    context = context,
                    size = 64,
                    onClick = onOpenFile
                )
            }
            
            // Content - File info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // File name with private badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = record.fileName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (record.isPrivate) {
                        PrivateBadge()
                    }
                }
                
                // Description (1-2 lines) or locked message
                if (isLockedPrivate) {
                    Text(
                        text = "🔒 Private - Break Glass to access",
                        fontSize = 12.sp,
                        color = PrivateColor,
                        fontWeight = FontWeight.Medium
                    )
                } else if (record.description.isNotBlank()) {
                    Text(
                        text = record.description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                }
                
                // Date and file size
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                            .format(record.uploadDate.toDate()),
                        fontSize = 11.sp,
                        color = AccentColor
                    )
                    
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    
                    Text(
                        text = record.getFormattedFileSize(),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
            
            // Action button - either Open or Glass Break
            if (isLockedPrivate && onGlassBreak != null) {
                // Glass break button for locked private records
                Surface(
                    onClick = { onGlassBreak() },
                    color = Color(0xFFFF5722), // Orange for glass break
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Break Glass",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Open button
                Surface(
                    onClick = onOpenFile,
                    color = HeaderTopColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Open",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Legacy compact card - kept for backward compatibility
 */
@Composable
fun CompactHealthRecordCard(
    record: HealthRecord,
    context: Context,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompactRecordCard(
        record = record,
        context = context,
        onOpenFile = onClick,
        onCardClick = onClick,
        modifier = modifier
    )
}

/**
 * Thumbnail component for health records
 * Shows image preview for images, PDF icon for PDFs
 */
@Composable
fun RecordThumbnail(
    record: HealthRecord,
    context: Context,
    size: Int = 64,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (record.isImage()) HeaderTopColor.copy(alpha = 0.05f)
                else if (record.isPdf()) PdfColor.copy(alpha = 0.08f)
                else HeaderTopColor.copy(alpha = 0.08f)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (record.isImage()) {
            // Image thumbnail with loading state
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(record.fileUrl)
                    .crossfade(true)
                    .size(size * 2) // Load at 2x for better quality
                    .build(),
                contentDescription = "Record preview",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AccentColor,
                            strokeWidth = 2.dp
                        )
                    }
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.BrokenImage,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size((size * 0.5).dp)
                    )
                }
            )
        } else {
            // PDF or other file icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when {
                        record.isPdf() -> Icons.Default.PictureAsPdf
                        else -> Icons.Default.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = if (record.isPdf()) PdfColor else AccentColor,
                    modifier = Modifier.size((size * 0.45).dp)
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = record.getFileExtension().uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (record.isPdf()) PdfColor else AccentColor
                )
            }
        }
    }
}

/**
 * Small private badge
 */
@Composable
private fun PrivateBadge() {
    Surface(
        color = PrivateColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(8.dp)
            )
            Text(
                text = "Private",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Bottom sheet for health record details
 * Shows full information when record card is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthRecordDetailsBottomSheet(
    record: HealthRecord,
    context: Context,
    isPatientView: Boolean = true,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onViewAccessLog: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Delete confirmation dialog
    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = PrivateColor,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { 
                Text(
                    "Delete Record?", 
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ) 
            },
            text = { 
                Text(
                    "This action cannot be undone. The record will be permanently deleted.",
                    textAlign = TextAlign.Center,
                    color = TextSecondary
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrivateColor)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceColor,
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp),
                    shape = RoundedCornerShape(2.dp),
                    color = DividerColor
                ) {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Record Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HeaderTopColor
                )
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }
            
            // File preview card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceColor)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Large preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                if (record.isImage()) Color.Black.copy(alpha = 0.03f)
                                else HeaderTopColor.copy(alpha = 0.05f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (record.isImage()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(record.fileUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Full preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                                loading = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(40.dp),
                                        color = AccentColor
                                    )
                                }
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (record.isPdf()) 
                                        Icons.Default.PictureAsPdf 
                                    else 
                                        Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (record.isPdf()) PdfColor else AccentColor,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = record.getFileExtension().uppercase() + " Document",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    
                    // File info row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = record.fileName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                if (record.isPrivate) {
                                    PrivateBadge()
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = record.getFormattedFileSize(),
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "•",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = record.fileType.ifBlank { 
                                        record.getFileExtension().uppercase() 
                                    },
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Open / Download button
                Button(
                    onClick = {
                        openRecordFile(context, record)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = HeaderTopColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open File", fontWeight = FontWeight.SemiBold)
                }
                
                // View access log (if available)
                if (onViewAccessLog != null && record.viewedBy.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onViewAccessLog,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${record.viewedBy.size} Views")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Details sections
            DetailsSection(title = "Upload Date") {
                Text(
                    text = SimpleDateFormat("EEEE, MMMM dd, yyyy 'at' h:mm a", Locale.getDefault())
                        .format(record.uploadDate.toDate()),
                    fontSize = 14.sp,
                    color = TextPrimary
                )
            }
            
            if (record.description.isNotBlank()) {
                DetailsSection(title = "Description") {
                    Text(
                        text = record.description,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }
            
            if (record.notes.isNotBlank()) {
                DetailsSection(title = "Notes") {
                    Text(
                        text = record.notes,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 20.sp
                    )
                }
            }
            
            if (record.pastMedication.isNotBlank()) {
                DetailsSection(title = "Past Medication") {
                    Text(
                        text = record.pastMedication,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }
            
            if (record.dependentName.isNotBlank()) {
                DetailsSection(title = "For Dependent") {
                    Text(
                        text = record.dependentName,
                        fontSize = 14.sp,
                        color = AccentColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Delete button (only for patient view with permission)
            if (isPatientView && onDelete != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PrivateColor
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(PrivateColor, PrivateColor))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Record", fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Section component for details view
 */
@Composable
private fun DetailsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentColor,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceColor)
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    }
}

/**
 * Opens a health record file in external app
 */
private fun openRecordFile(context: Context, record: HealthRecord) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(record.fileUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("HealthRecordComponents", "Error opening file: ${e.message}")
        android.widget.Toast.makeText(
            context,
            "Cannot open file. Please try again.",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Wrapper for RecordDetailsBottomSheet with simplified API
 * Used by various screens for record details display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailsBottomSheet(
    record: HealthRecord,
    context: Context,
    onDismiss: () -> Unit,
    onOpenFile: () -> Unit,
    onDownload: () -> Unit,
    onDelete: (() -> Unit)?,
    onViewAccessLog: (() -> Unit)?,
    showDeleteButton: Boolean = false
) {
    HealthRecordDetailsBottomSheet(
        record = record,
        context = context,
        isPatientView = showDeleteButton,
        onDismiss = onDismiss,
        onDelete = if (showDeleteButton) onDelete else null,
        onViewAccessLog = onViewAccessLog
    )
}
