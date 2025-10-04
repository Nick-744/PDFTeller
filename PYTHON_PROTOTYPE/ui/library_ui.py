import tkinter as tk
from tkinter import ttk, messagebox
from typing import Callable
import sys
from pathlib import Path

# Add parent directory to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from library_manager import LibraryManager, Book, Checkpoint


class LibraryUI:
    def __init__(
        self,
        library_manager: LibraryManager,
        on_book_loaded: Callable[[Book, list[str]], None],
        on_checkpoint_loaded: Callable[[Checkpoint, list[str]], None]
    ):
        self.library_manager = library_manager
        self.on_book_loaded = on_book_loaded
        self.on_checkpoint_loaded = on_checkpoint_loaded

    def show_library(self, parent: tk.Tk):
        # Create library window
        dialog = tk.Toplevel(parent)
        dialog.title("Library")
        dialog.geometry("900x600")
        dialog.transient(parent)
        dialog.grab_set()

        # Create notebook for tabs
        notebook = ttk.Notebook(dialog)
        notebook.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        # Books tab
        books_tab = tk.Frame(notebook)
        notebook.add(books_tab, text="Books")
        self.create_books_tab(books_tab, dialog)

        # Checkpoints tab
        checkpoints_tab = tk.Frame(notebook)
        notebook.add(checkpoints_tab, text="Checkpoints")
        self.create_checkpoints_tab(checkpoints_tab, dialog)

    def create_books_tab(self, parent: tk.Frame, dialog: tk.Toplevel):
        # Listbox with scrollbar
        list_frame = tk.Frame(parent)
        list_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        scrollbar = tk.Scrollbar(list_frame)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        books_listbox = tk.Listbox(
            list_frame,
            yscrollcommand=scrollbar.set,
            font=("Arial", 10),
            height=20
        )
        books_listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.config(command=books_listbox.yview)

        # Populate listbox
        for book in self.library_manager.library:
            display_text = (
                f"{book.title}\n"
                f"  {book.sentence_count} sentences\n"
                f"  Added: {book.added_date.strftime('%Y-%m-%d %H:%M')}\n"
            )
            books_listbox.insert(tk.END, display_text)

        # Button bar
        button_frame = tk.Frame(parent)
        button_frame.pack(pady=10)

        def load_selected():
            selection = books_listbox.curselection()
            if selection:
                idx = selection[0] // 4  # Account for multi-line items
                # Find actual index by counting non-empty items
                actual_idx = 0
                item_count = 0
                for i in range(books_listbox.size()):
                    text = books_listbox.get(i)
                    if text and not text.startswith("  "):
                        if item_count == selection[0] // 4:
                            actual_idx = item_count
                            break
                        item_count += 1

                # Simpler approach: track books by their position
                books = self.library_manager.library
                selected_idx = 0
                current_line = 0
                for i, book in enumerate(books):
                    if current_line <= selection[0] < current_line + 4:
                        selected_idx = i
                        break
                    current_line += 4

                book = books[selected_idx]
                sentences = self.library_manager.load_book_from_library(book)
                if sentences:
                    self.on_book_loaded(book, sentences)
                    dialog.destroy()

        def delete_selected():
            selection = books_listbox.curselection()
            if selection:
                books = self.library_manager.library
                selected_idx = 0
                current_line = 0
                for i, book in enumerate(books):
                    if current_line <= selection[0] < current_line + 4:
                        selected_idx = i
                        break
                    current_line += 4

                book = books[selected_idx]
                result = messagebox.askyesno(
                    "Delete Book",
                    f"Delete \"{book.title}\"?\nThis action cannot be undone."
                )
                if result:
                    self.library_manager.delete_book(book)
                    dialog.destroy()
                    # Reopen dialog to refresh
                    self.show_library(dialog.master)

        load_button = tk.Button(
            button_frame,
            text="Load Selected",
            width=12,
            command=load_selected
        )
        load_button.pack(side=tk.LEFT, padx=5)

        delete_button = tk.Button(
            button_frame,
            text="Delete",
            width=12,
            command=delete_selected
        )
        delete_button.pack(side=tk.LEFT, padx=5)

        close_button = tk.Button(
            button_frame,
            text="Close",
            width=12,
            command=dialog.destroy
        )
        close_button.pack(side=tk.LEFT, padx=5)

    def create_checkpoints_tab(self, parent: tk.Frame, dialog: tk.Toplevel):
        # Listbox with scrollbar
        list_frame = tk.Frame(parent)
        list_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)

        scrollbar = tk.Scrollbar(list_frame)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)

        checkpoints_listbox = tk.Listbox(
            list_frame,
            yscrollcommand=scrollbar.set,
            font=("Arial", 10),
            height=20
        )
        checkpoints_listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.config(command=checkpoints_listbox.yview)

        # Populate listbox
        for checkpoint in self.library_manager.checkpoints:
            display_text = (
                f"{checkpoint.book_title}\n"
                f"  At sentence: {checkpoint.sentence_index + 1}\n"
                f"  Saved: {checkpoint.saved_date.strftime('%Y-%m-%d %H:%M')}\n"
            )
            checkpoints_listbox.insert(tk.END, display_text)

        # Button bar
        button_frame = tk.Frame(parent)
        button_frame.pack(pady=10)

        def load_selected():
            selection = checkpoints_listbox.curselection()
            if selection:
                checkpoints = self.library_manager.checkpoints
                selected_idx = 0
                current_line = 0
                for i, checkpoint in enumerate(checkpoints):
                    if current_line <= selection[0] < current_line + 4:
                        selected_idx = i
                        break
                    current_line += 4

                checkpoint = checkpoints[selected_idx]
                sentences = self.library_manager.load_checkpoint(checkpoint)
                if sentences:
                    self.on_checkpoint_loaded(checkpoint, sentences)
                    dialog.destroy()

        def delete_selected():
            selection = checkpoints_listbox.curselection()
            if selection:
                checkpoints = self.library_manager.checkpoints
                selected_idx = 0
                current_line = 0
                for i, checkpoint in enumerate(checkpoints):
                    if current_line <= selection[0] < current_line + 4:
                        selected_idx = i
                        break
                    current_line += 4

                checkpoint = checkpoints[selected_idx]
                self.library_manager.delete_checkpoint(checkpoint)
                dialog.destroy()
                # Reopen dialog to refresh
                self.show_library(dialog.master)

        load_button = tk.Button(
            button_frame,
            text="Load Checkpoint",
            width=13,
            command=load_selected
        )
        load_button.pack(side=tk.LEFT, padx=5)

        delete_button = tk.Button(
            button_frame,
            text="Delete",
            width=12,
            command=delete_selected
        )
        delete_button.pack(side=tk.LEFT, padx=5)

        close_button = tk.Button(
            button_frame,
            text="Close",
            width=12,
            command=dialog.destroy
        )
        close_button.pack(side=tk.LEFT, padx=5)
