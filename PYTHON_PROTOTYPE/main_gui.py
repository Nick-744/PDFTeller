import tkinter as tk
from tkinter import filedialog
import threading
from pathlib import Path
from typing import Optional

from utils.process_pdf import process_pdf_text_with_structure
from utils.tts import speak_text
from library_manager import LibraryManager, Book, Checkpoint
from ui.top_half import TopHalf
from ui.bottom_half import BottomHalf
from ui.library_ui import LibraryUI

class MainApp:
    def __init__(self, root: tk.Tk):
        self.root = root
        self.root.title("PDFTeller")

        # Maximize window
        self.root.state('zoomed')  # Windows

        # Get screen height
        screen_height = root.winfo_screenheight()
        half_height = screen_height // 2

        # State management
        self.is_playing = False
        self.is_stopped = True
        self.stop_requested = False
        self.current_index = 0
        self.current_sentences: list[str] = []
        self.current_book_title: Optional[str] = None

        # Library manager
        self.library_manager = LibraryManager()

        # Create PanedWindow to split the window in half
        paned_window = tk.PanedWindow(root, orient=tk.VERTICAL, sashwidth=5)
        paned_window.pack(fill=tk.BOTH, expand=True)

        # Create UI components
        self.top_half = TopHalf(
            paned_window,
            on_play=self.handle_play,
            on_stop=self.handle_stop,
            on_save_checkpoint=self.save_checkpoint,
            on_toggle_dyslexia=self.toggle_dyslexia_mode
        )
        paned_window.add(self.top_half.container, height=half_height)

        self.bottom_half = BottomHalf(
            paned_window,
            on_load_pdf=self.load_pdf_file,
            on_show_library=self.show_library
        )
        paned_window.add(self.bottom_half.container, height=half_height)

        self.library_ui = LibraryUI(
            self.library_manager,
            on_book_loaded=self.load_book_from_library,
            on_checkpoint_loaded=self.load_checkpoint
        )

    def toggle_dyslexia_mode(self, enabled: bool):
        self.top_half.toggle_dyslexia_mode(enabled)

    def handle_play(self):
        if not self.current_sentences:
            self.bottom_half.update_status("No PDF loaded")
            return

        self.is_playing = True
        self.is_stopped = False
        self.stop_requested = False
        self.update_button_states()

        # Start speaking in a separate thread
        threading.Thread(target=self.speak_sentences, daemon=True).start()

    def handle_stop(self):
        self.stop_requested = True
        self.bottom_half.update_status("Finishing current sentence...")
        self.top_half.stop_button.config(state=tk.DISABLED)

    def update_button_states(self):
        self.top_half.update_button_states(
            self.is_playing,
            self.current_sentences,
            self.current_book_title
        )

    def save_checkpoint(self):
        if self.current_book_title is None:
            return

        # Find the book's file path
        book = next(
            (b for b in self.library_manager.library if b.title == self.current_book_title),
            None
        )
        if book is None:
            return

        success = self.library_manager.save_checkpoint(
            self.current_book_title,
            self.current_index,
            book.file_path
        )

        if success:
            self.bottom_half.update_status(f"Checkpoint saved at sentence {self.current_index + 1}")
            self.show_temporary_message()
        else:
            self.bottom_half.update_status("Failed to save checkpoint")

    def show_temporary_message(self):
        original_text = self.bottom_half.status_label.cget("text")
        message = "✓ Checkpoint saved!"
        self.bottom_half.update_status(message)

        def restore_message():
            if self.bottom_half.status_label.cget("text") == message:
                self.bottom_half.update_status(original_text)

        self.root.after(2000, restore_message)

    def load_pdf_file(self):
        file_path = filedialog.askopenfilename(
            title="Select a PDF document",
            filetypes=[("PDF documents", "*.pdf")]
        )

        if file_path:
            self.process_file(Path(file_path))

    def process_file(self, file_path: Path):
        self.bottom_half.update_status("Processing PDF...")
        self.bottom_half.clear_sentences()
        self.top_half.update_current_sentence("Loading...")

        def process():
            sentences = process_pdf_text_with_structure(str(file_path))
            self.current_sentences = sentences
            self.current_index = 0

            # Save to library
            book_title = file_path.stem
            self.current_book_title = book_title
            self.save_book_to_library(book_title, sentences, file_path.name)

            self.root.after(0, lambda: self.bottom_half.update_status(f"PDF loaded ({len(sentences)} sentences)"))
            self.root.after(0, lambda: self.top_half.update_current_sentence("Ready to play"))
            self.root.after(0, self.update_button_states)

        threading.Thread(target=process, daemon=True).start()

    def save_book_to_library(self, title: str, sentences: list[str], original_pdf_name: str):
        success = self.library_manager.save_book_to_library(title, sentences, original_pdf_name)
        if not success:
            self.root.after(0, lambda: self.bottom_half.update_status("Failed to save to library"))

    def show_library(self):
        self.library_ui.show_library(self.root)

    def load_checkpoint(self, checkpoint: Checkpoint, sentences: list[str]):
        self.current_sentences = sentences
        self.current_index = checkpoint.sentence_index
        self.current_book_title = checkpoint.book_title

        self.bottom_half.clear_sentences()

        # Add previous sentences to history
        previous_sentences = sentences[:checkpoint.sentence_index]
        self.bottom_half.add_sentences(previous_sentences)

        self.bottom_half.update_status(f"Loaded checkpoint: {checkpoint.book_title} at sentence {checkpoint.sentence_index + 1}")
        self.top_half.update_current_sentence("Ready to resume from checkpoint")
        self.update_button_states()

    def load_book_from_library(self, book: Book, sentences: list[str]):
        self.current_sentences = sentences
        self.current_index = 0
        self.current_book_title = book.title

        self.bottom_half.clear_sentences()
        self.bottom_half.update_status(f"Loaded: {book.title} ({len(sentences)} sentences)")
        self.top_half.update_current_sentence("Ready to play")
        self.update_button_states()

    def speak_sentences(self):
        self.root.after(0, lambda: self.bottom_half.update_status("Playing..."))

        while self.current_index < len(self.current_sentences) and self.is_playing:
            sentence = self.current_sentences[self.current_index]

            self.root.after(0, lambda s=sentence: self.top_half.update_current_sentence(s))
            self.root.after(0, lambda s=sentence: self.bottom_half.add_sentence(s))

            # Speak the sentence (blocking call)
            speak_text(sentence)

            # After sentence completes, check if stop was requested
            if self.stop_requested:
                self.root.after(0, lambda: self.top_half.update_current_sentence("Stopped"))
                self.root.after(0, lambda: self.bottom_half.update_status("Stopped"))
                self.is_playing = False
                self.is_stopped = True
                self.stop_requested = False
                self.current_index += 1  # Move to next sentence for resume
                self.root.after(0, self.update_button_states)
                return

            self.current_index += 1

            # Small delay between sentences
            import time
            time.sleep(0.2)

        # Check if we finished all sentences
        if self.current_index >= len(self.current_sentences):
            self.root.after(0, lambda: self.top_half.update_current_sentence("Completed"))
            self.root.after(0, lambda: self.bottom_half.update_status("Finished"))
            self.current_index = 0  # Reset for replay
            self.is_playing = False
            self.is_stopped = True
            self.stop_requested = False
            self.root.after(0, self.update_button_states)


def main():
    root = tk.Tk()
    app = MainApp(root)
    root.mainloop()


if __name__ == '__main__':
    main()
