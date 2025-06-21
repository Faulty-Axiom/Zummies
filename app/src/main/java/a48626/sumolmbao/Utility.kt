package a48626.sumolmbao

import android.content.Context
import android.widget.Toast
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

object Utility {

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun timestampToString(timestamp: Timestamp): String {
        return SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(timestamp.toDate())
    }
}