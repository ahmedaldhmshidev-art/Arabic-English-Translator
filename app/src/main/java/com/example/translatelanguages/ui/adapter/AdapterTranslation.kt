package com.example.translatelanguages.ui.adapter

import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.TextView
    import androidx.recyclerview.widget.RecyclerView
    import com.example.translatelanguages.R
import com.example.translatelanguages.data.local.database.EntityTranslate
import com.example.translatelanguages.utils.formatTime
import java.text.SimpleDateFormat
    import java.util.*
enum class SelectState {Delete ,Data}
class AdapterTranslate(
    private val onClickData: (EntityTranslate, action: SelectState) -> Unit
    ) : RecyclerView.Adapter<AdapterTranslate.TranslateViewHolder>() {
        private var list: List<EntityTranslate> = emptyList()
        // تحديث القائمة
    fun submitList(newList: List<EntityTranslate>) {
            list = newList
            notifyDataSetChanged()
        }
        // ViewHolder
        inner class TranslateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvOriginal: TextView = itemView.findViewById(R.id.tvOriginalId_item)
            val tvLanguages: TextView = itemView.findViewById(R.id.tvLanguagesId_item)
            val tvTranslated: TextView = itemView.findViewById(R.id.tvTranslated_id_item)
            val tvTime: TextView = itemView.findViewById(R.id.tvTimestampId_item)

            fun bind(item: EntityTranslate) {
                tvOriginal.text = item.originalTextEntity
                tvTranslated.text = item.translatedTextEntity
                tvLanguages.text = "${item.fromLanguageEntity} → ${item.toLanguageEntity}"
                tvTime.text = formatTime(item.timesTampEntity)

                // الضغط المطوّل للحذف
                itemView.setOnLongClickListener {
                    onClickData(item , SelectState.Delete)
                    true
                }
                itemView.setOnClickListener {
                    onClickData(item , SelectState.Data)
                }
            }
        }
        // إنشاء ViewHolder
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TranslateViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_translate, parent, false)
            return TranslateViewHolder(view)
        }
        // ربط البيانات
        override fun onBindViewHolder(holder: TranslateViewHolder, position: Int) {
            holder.bind(list[position])
        }
        // عدد العناصر
        override fun getItemCount(): Int = list.size
    }
