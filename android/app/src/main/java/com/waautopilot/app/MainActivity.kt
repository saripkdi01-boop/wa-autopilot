package com.waautopilot.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.waautopilot.app.databinding.ActivityMainBinding
import com.google.android.material.textfield.TextInputEditText

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: RuleViewModel
    private lateinit var adapter: RuleAdapter

    // Ganti dengan user ID dari backend setelah register
    private val userId = "USER_ID_DARI_BACKEND"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[RuleViewModel::class.java]

        adapter = RuleAdapter { rule ->
            confirmDelete(rule)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.rules.observe(this) { rules ->
            adapter.submitList(rules)
        }

        viewModel.stats.observe(this) { stats ->
            binding.tvStats.text = "Rules: ${stats.total_rules} | Replies: ${stats.total_replies}"
        }

        binding.fabAdd.setOnClickListener {
            showAddRuleDialog()
        }

        viewModel.loadRules(userId)
        viewModel.loadStats(userId)
    }

    private fun showAddRuleDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_rule, null)
        val etKeyword = dialogView.findViewById<TextInputEditText>(R.id.etKeyword)
        val etReply = dialogView.findViewById<TextInputEditText>(R.id.etReply)

        AlertDialog.Builder(this)
            .setTitle("Tambah Auto-Reply")
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val keyword = etKeyword.text.toString().trim()
                val reply = etReply.text.toString().trim()

                if (keyword.isNotEmpty() && reply.isNotEmpty()) {
                    viewModel.addRule(userId, keyword, reply)
                } else {
                    Toast.makeText(this, "Isi semua field", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun confirmDelete(rule: Rule) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Rule")
            .setMessage("Hapus rule '${rule.keyword}'?")
            .setPositiveButton("Hapus") { _, _ ->
                viewModel.deleteRule(userId, rule.id)
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
