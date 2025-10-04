import tkinter as tk
from tkinter import ttk
from typing import Callable


class BottomHalf:
    def __init__(
        self,
        parent: tk.Tk,
        on_load_pdf: Callable[[], None],
        on_show_library: Callable[[], None]
    ):
        self.parent = parent
        self.on_load_pdf = on_load_pdf
        self.on_show_library = on_show_library

        # Create bottom half container - takes half the window
        self.container = tk.Frame(parent)

        # Bottom bar with status and buttons - pack first
        bottom_bar = tk.Frame(self.container)
        bottom_bar.pack(side=tk.BOTTOM, fill=tk.X, pady=10)

        # Sentence history - use Text widget for word wrapping
        list_frame = tk.Frame(self.container)
        list_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        scrollbar = tk.Scrollbar(list_frame)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        self.sentence_text = tk.Text(
            list_frame,
            yscrollcommand=scrollbar.set,
            font=("Arial", 10),
            wrap=tk.WORD,
            state=tk.DISABLED,
            spacing3=10  # Add 10 pixels of space after each line
        )
        self.sentence_text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.config(command=self.sentence_text.yview)

        self.load_button = tk.Button(
            bottom_bar,
            text="Load PDF",
            command=self.on_load_pdf
        )
        self.load_button.pack(side=tk.LEFT, padx=10)

        self.library_button = tk.Button(
            bottom_bar,
            text="📚 Library",
            command=self.on_show_library
        )
        self.library_button.pack(side=tk.LEFT, padx=5)

        self.status_label = tk.Label(
            bottom_bar,
            text="Ready for your pdf!",
            anchor=tk.W
        )
        self.status_label.pack(side=tk.LEFT, padx=10)

    def update_status(self, text: str):
        self.status_label.config(text=text)

    def clear_sentences(self):
        self.sentence_text.config(state=tk.NORMAL)
        self.sentence_text.delete("1.0", tk.END)
        self.sentence_text.config(state=tk.DISABLED)

    def add_sentence(self, sentence: str):
        self.sentence_text.config(state=tk.NORMAL)
        self.sentence_text.insert(tk.END, sentence + "\n")
        self.sentence_text.see(tk.END)  # Auto-scroll to latest
        self.sentence_text.config(state=tk.DISABLED)

    def add_sentences(self, sentences: list[str]):
        self.sentence_text.config(state=tk.NORMAL)
        for sentence in sentences:
            self.sentence_text.insert(tk.END, sentence + "\n")
        if sentences:
            self.sentence_text.see(tk.END)
        self.sentence_text.config(state=tk.DISABLED)
