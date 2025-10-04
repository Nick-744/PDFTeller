import tkinter as tk
from tkinter import ttk
from typing import Callable, Optional


class TopHalf:
    def __init__(
        self,
        parent: tk.Tk,
        on_play: Callable[[], None],
        on_stop: Callable[[], None],
        on_save_checkpoint: Callable[[], None],
        on_toggle_dyslexia: Callable[[bool], None]
    ):
        self.parent = parent
        self.on_play = on_play
        self.on_stop = on_stop
        self.on_save_checkpoint = on_save_checkpoint
        self.on_toggle_dyslexia = on_toggle_dyslexia

        # Create top half container - takes half the window
        self.container = tk.Frame(parent, relief=tk.GROOVE, borderwidth=1)

        # Control buttons frame - pack first to ensure visibility
        control_frame = tk.Frame(self.container)
        control_frame.pack(side=tk.BOTTOM, pady=10)

        # Current sentence display - scrollable text widget
        self.sentence_frame = tk.Frame(self.container)
        self.sentence_frame.pack(fill=tk.BOTH, expand=True, padx=20, pady=20)

        # Scrollbar for text widget
        scrollbar = tk.Scrollbar(self.sentence_frame)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        # Use Text widget instead of Label for better space utilization
        self.current_sentence_text = tk.Text(
            self.sentence_frame,
            font=("Arial", 24),
            wrap=tk.WORD,
            yscrollcommand=scrollbar.set,
            relief=tk.FLAT,
            state=tk.DISABLED
        )
        self.current_sentence_text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.config(command=self.current_sentence_text.yview)

        # Set initial text
        self.current_sentence_text.config(state=tk.NORMAL)
        self.current_sentence_text.insert("1.0", "No sentence playing...")
        self.current_sentence_text.config(state=tk.DISABLED)

        self.base_sentence_bg = self.current_sentence_text.cget("background")
        self.base_sentence_fg = self.current_sentence_text.cget("foreground")

        self.base_container_bg = self.container.cget("background")

        self.play_button = tk.Button(
            control_frame,
            text="▶ Play",
            width=10,
            command=self.on_play
        )
        self.play_button.pack(side=tk.LEFT, padx=5)

        self.stop_button = tk.Button(
            control_frame,
            text="■ Stop",
            width=10,
            state=tk.DISABLED,
            command=self.on_stop
        )
        self.stop_button.pack(side=tk.LEFT, padx=5)

        self.checkpoint_button = tk.Button(
            control_frame,
            text="🔖 Save Checkpoint",
            width=16,
            state=tk.DISABLED,
            command=self.on_save_checkpoint
        )
        self.checkpoint_button.pack(side=tk.LEFT, padx=5)

        # Dyslexia-friendly toggle
        self.dyslexia_var = tk.BooleanVar()
        self.dyslexia_toggle = tk.Checkbutton(
            control_frame,
            text="Dyslexia-friendly",
            variable=self.dyslexia_var,
            command=lambda: self.on_toggle_dyslexia(self.dyslexia_var.get())
        )
        self.dyslexia_toggle.pack(side=tk.LEFT, padx=5)

    def toggle_dyslexia_mode(self, enabled: bool):
        if enabled:
            self.container.config(bg="#1d0f0f")
            self.sentence_frame.config(bg="#1d0f0f")
            self.current_sentence_text.config(
                bg="#1d0f0f",
                fg="#a08060",
                font=("OpenDyslexic", 24)
            )
        else:
            # Restore base styles
            self.container.config(bg=self.base_container_bg)
            self.sentence_frame.config(bg=self.base_container_bg)
            self.current_sentence_text.config(
                bg=self.base_sentence_bg,
                fg=self.base_sentence_fg,
                font=("Arial", 24)
            )

    def update_button_states(
        self,
        is_playing: bool,
        current_sentences: list[str],
        current_book_title: Optional[str]
    ):
        self.play_button.config(
            state=tk.DISABLED if is_playing or not current_sentences else tk.NORMAL
        )
        self.stop_button.config(
            state=tk.NORMAL if is_playing else tk.DISABLED
        )
        self.checkpoint_button.config(
            state=tk.NORMAL if current_sentences and current_book_title else tk.DISABLED
        )

    def update_current_sentence(self, text: str):
        self.current_sentence_text.config(state=tk.NORMAL)
        self.current_sentence_text.delete("1.0", tk.END)
        self.current_sentence_text.insert("1.0", text)
        self.current_sentence_text.config(state=tk.DISABLED)
