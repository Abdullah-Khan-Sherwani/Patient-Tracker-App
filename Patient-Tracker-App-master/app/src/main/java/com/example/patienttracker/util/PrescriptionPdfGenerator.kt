package com.example.patienttracker.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.patienttracker.data.DoctorNote
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Utility class for generating prescription PDFs
 */
object PrescriptionPdfGenerator {
    
    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 50f
    
    /**
     * Generate a prescription PDF from a doctor's note
     * @param context Android context
     * @param note The doctor's note to generate PDF from
     * @return File path of the generated PDF, or null if failed
     */
    fun generatePrescriptionPdf(context: Context, note: DoctorNote): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            drawPrescriptionContent(canvas, note)
            
            pdfDocument.finishPage(page)
            
            // Save to cache directory for sharing
            val fileName = "Prescription_${note.patientName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            
            pdfDocument.close()
            
            android.util.Log.d("PdfGenerator", "PDF generated: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "Failed to generate PDF: ${e.message}", e)
            null
        }
    }
    
    private fun drawPrescriptionContent(canvas: Canvas, note: DoctorNote) {
        var yPosition = MARGIN + 30f
        
        // Title Paint
        val titlePaint = Paint().apply {
            color = Color.parseColor("#04645A") // Teal color matching app theme
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        
        // Subtitle Paint
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            textAlign = Paint.Align.CENTER
        }
        
        // Header Paint
        val headerPaint = Paint().apply {
            color = Color.parseColor("#04786A")
            textSize = 16f
            isFakeBoldText = true
        }
        
        // Body Paint
        val bodyPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
        }
        
        // Light gray paint for boxes
        val boxPaint = Paint().apply {
            color = Color.parseColor("#F0F0F0")
            style = Paint.Style.FILL
        }
        
        // Border paint
        val borderPaint = Paint().apply {
            color = Color.parseColor("#04645A")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        // ============ HEADER ============
        // Hospital Name
        canvas.drawText("IAT Hospital", PAGE_WIDTH / 2f, yPosition, titlePaint)
        yPosition += 25f
        
        // Subtitle
        canvas.drawText("Medical Prescription", PAGE_WIDTH / 2f, yPosition, subtitlePaint)
        yPosition += 15f
        
        // Decorative line
        val linePaint = Paint().apply {
            color = Color.parseColor("#04645A")
            strokeWidth = 2f
        }
        canvas.drawLine(MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition, linePaint)
        yPosition += 30f
        
        // ============ PATIENT INFO BOX ============
        canvas.drawRect(MARGIN, yPosition - 5f, PAGE_WIDTH - MARGIN, yPosition + 70f, boxPaint)
        canvas.drawRect(MARGIN, yPosition - 5f, PAGE_WIDTH - MARGIN, yPosition + 70f, borderPaint)
        
        yPosition += 15f
        canvas.drawText("Patient Name:", MARGIN + 10f, yPosition, headerPaint)
        canvas.drawText(note.patientName, MARGIN + 130f, yPosition, bodyPaint)
        
        yPosition += 25f
        canvas.drawText("Date:", MARGIN + 10f, yPosition, headerPaint)
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        canvas.drawText(dateFormat.format(note.appointmentDate.toDate()), MARGIN + 130f, yPosition, bodyPaint)
        
        yPosition += 25f
        canvas.drawText("Department:", MARGIN + 10f, yPosition, headerPaint)
        canvas.drawText(note.speciality, MARGIN + 130f, yPosition, bodyPaint)
        
        yPosition += 40f
        
        // ============ DIAGNOSIS/COMMENTS SECTION ============
        canvas.drawText("Doctor's Comments / Diagnosis:", MARGIN, yPosition, headerPaint)
        yPosition += 25f
        
        // Draw comments with word wrap
        yPosition = drawWrappedText(canvas, note.comments.ifBlank { "No comments provided" }, 
            MARGIN, yPosition, PAGE_WIDTH - 2 * MARGIN, bodyPaint)
        
        yPosition += 30f
        
        // ============ PRESCRIPTION SECTION ============
        val rxPaint = Paint().apply {
            color = Color.parseColor("#04645A")
            textSize = 36f
            isFakeBoldText = true
        }
        canvas.drawText("℞", MARGIN, yPosition + 10f, rxPaint)
        
        canvas.drawText("Prescription:", MARGIN + 50f, yPosition, headerPaint)
        yPosition += 25f
        
        // Draw prescription box
        val prescriptionBoxTop = yPosition - 10f
        val prescriptionText = note.prescription.ifBlank { "No prescription provided" }
        
        // Draw prescription with word wrap
        yPosition = drawWrappedText(canvas, prescriptionText, 
            MARGIN + 10f, yPosition, PAGE_WIDTH - 2 * MARGIN - 20f, bodyPaint)
        
        yPosition += 20f
        
        // Draw box around prescription
        canvas.drawRect(MARGIN, prescriptionBoxTop, PAGE_WIDTH - MARGIN, yPosition, boxPaint)
        canvas.drawRect(MARGIN, prescriptionBoxTop, PAGE_WIDTH - MARGIN, yPosition, borderPaint)
        
        // Redraw text on top of box
        yPosition = prescriptionBoxTop + 20f
        yPosition = drawWrappedText(canvas, prescriptionText, 
            MARGIN + 10f, yPosition, PAGE_WIDTH - 2 * MARGIN - 20f, bodyPaint)
        
        // ============ FOOTER / SIGNATURE SECTION ============
        val footerY = PAGE_HEIGHT - MARGIN - 80f
        
        // Signature line
        val signaturePaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1f
        }
        canvas.drawLine(PAGE_WIDTH - MARGIN - 200f, footerY, PAGE_WIDTH - MARGIN, footerY, signaturePaint)
        
        // Doctor name
        val doctorNamePaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Dr. ${note.doctorName}", PAGE_WIDTH - MARGIN, footerY + 20f, doctorNamePaint)
        
        // Specialty
        val specialtyPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(note.speciality, PAGE_WIDTH - MARGIN, footerY + 35f, specialtyPaint)
        
        // Disclaimer
        val disclaimerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        canvas.drawText("This prescription does not require a signature.", PAGE_WIDTH - MARGIN, footerY + 55f, disclaimerPaint)
        
        // Footer line
        canvas.drawLine(MARGIN, PAGE_HEIGHT - MARGIN - 10f, PAGE_WIDTH - MARGIN, PAGE_HEIGHT - MARGIN - 10f, linePaint)
        
        // Hospital contact (optional)
        val footerTextPaint = Paint().apply {
            color = Color.GRAY
            textSize = 10f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("IAT Hospital | Generated via Medify App", PAGE_WIDTH / 2f, PAGE_HEIGHT - MARGIN, footerTextPaint)
    }
    
    /**
     * Draw text with word wrapping
     * @return The Y position after drawing all text
     */
    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint
    ): Float {
        var yPosition = startY
        val lineHeight = paint.textSize + 6f
        
        val words = text.split(" ")
        var currentLine = StringBuilder()
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            val textWidth = paint.measureText(testLine)
            
            if (textWidth > maxWidth && currentLine.isNotEmpty()) {
                canvas.drawText(currentLine.toString(), x, yPosition, paint)
                yPosition += lineHeight
                currentLine = StringBuilder(word)
            } else {
                currentLine = StringBuilder(testLine)
            }
        }
        
        // Draw remaining text
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine.toString(), x, yPosition, paint)
            yPosition += lineHeight
        }
        
        return yPosition
    }
    
    /**
     * Open PDF file using system viewer
     */
    fun openPdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "Failed to open PDF: ${e.message}", e)
            android.widget.Toast.makeText(context, "No PDF viewer found", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Share PDF file
     */
    fun sharePdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            
            context.startActivity(Intent.createChooser(intent, "Share Prescription"))
        } catch (e: Exception) {
            android.util.Log.e("PdfGenerator", "Failed to share PDF: ${e.message}", e)
        }
    }
}
