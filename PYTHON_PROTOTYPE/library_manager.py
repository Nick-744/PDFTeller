from dataclasses import dataclass
from datetime import datetime
from pathlib import Path
from typing import Optional
import json
import os


@dataclass
class Book:
    title: str
    sentence_count: int
    file_path: str
    added_date: datetime
    original_pdf_name: str


@dataclass
class Checkpoint:
    book_title: str
    sentence_index: int
    saved_date: datetime
    file_path: str


class LibraryManager:
    def __init__(self):
        self.library: list[Book] = []
        self.checkpoints: list[Checkpoint] = []

        self.library_dir = Path.home() / ".pdfteller_library"

        if not self.library_dir.exists():
            self.library_dir.mkdir(parents=True)

        self.load_library()
        self.load_checkpoints()

    def _parse_datetime(self, date_str: str) -> datetime:
        """Parse datetime string, handling high-precision microseconds"""
        # Handle microseconds with more than 6 digits
        if '.' in date_str:
            base, microseconds = date_str.rsplit('.', 1)
            # Truncate microseconds to 6 digits
            microseconds = microseconds[:6]
            date_str = f"{base}.{microseconds}"
        return datetime.fromisoformat(date_str)

    def save_book_to_library(self, title: str, sentences: list[str], original_pdf_name: str) -> bool:
        # Check if book already exists (same title and sentence count)
        existing_book = next(
            (book for book in self.library
             if book.title == title and book.sentence_count == len(sentences)),
            None
        )

        if existing_book is not None:
            return False  # Book already exists

        # Create unique filename
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        file_name = f"{title}_{timestamp}.txt"
        file_path = str(self.library_dir / file_name)

        # Save sentences to file
        try:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write('\n'.join(sentences))

            # Add to library
            book = Book(
                title=title,
                sentence_count=len(sentences),
                file_path=file_path,
                added_date=datetime.now(),
                original_pdf_name=original_pdf_name
            )

            self.library.append(book)
            self.save_library_metadata()
            return True
        except Exception as e:
            print(f"Error saving book: {e}")
            return False

    def save_checkpoint(self, book_title: str, sentence_index: int, file_path: str) -> bool:
        try:
            checkpoint = Checkpoint(
                book_title=book_title,
                sentence_index=sentence_index,
                saved_date=datetime.now(),
                file_path=file_path
            )

            # Remove old checkpoints for the same book
            self.checkpoints = [cp for cp in self.checkpoints if cp.book_title != book_title]
            self.checkpoints.append(checkpoint)
            self.save_checkpoints_metadata()
            return True
        except Exception as e:
            print(f"Error saving checkpoint: {e}")
            return False

    def load_library(self):
        metadata_file = self.library_dir / "library.json"
        if not metadata_file.exists():
            return

        try:
            with open(metadata_file, 'r', encoding='utf-8') as f:
                lines = f.readlines()

            for line in lines:
                if "title:" in line:
                    parts = line.strip().split("|")
                    if len(parts) >= 5:
                        book = Book(
                            title=parts[0].split("title:")[1],
                            sentence_count=int(parts[1].split("count:")[1]),
                            file_path=parts[2].split("path:")[1],
                            added_date=self._parse_datetime(parts[3].split("date:")[1]),
                            original_pdf_name=parts[4].split("pdf:")[1]
                        )
                        if Path(book.file_path).exists():
                            self.library.append(book)
        except Exception as e:
            print(f"Error loading library: {e}")

    def save_library_metadata(self):
        metadata_file = self.library_dir / "library.json"
        try:
            content = '\n'.join([
                f"title:{book.title}|count:{book.sentence_count}|path:{book.file_path}|date:{book.added_date.isoformat()}|pdf:{book.original_pdf_name}"
                for book in self.library
            ])
            with open(metadata_file, 'w', encoding='utf-8') as f:
                f.write(content)
        except Exception as e:
            print(f"Error saving library metadata: {e}")

    def load_checkpoints(self):
        checkpoints_file = self.library_dir / "checkpoints.txt"
        if not checkpoints_file.exists():
            return

        try:
            with open(checkpoints_file, 'r', encoding='utf-8') as f:
                lines = f.readlines()

            for line in lines:
                if "title:" in line:
                    parts = line.strip().split("|")
                    if len(parts) >= 4:
                        checkpoint = Checkpoint(
                            book_title=parts[0].split("title:")[1],
                            sentence_index=int(parts[1].split("index:")[1]),
                            saved_date=self._parse_datetime(parts[2].split("date:")[1]),
                            file_path=parts[3].split("path:")[1]
                        )
                        if Path(checkpoint.file_path).exists():
                            self.checkpoints.append(checkpoint)
        except Exception as e:
            print(f"Error loading checkpoints: {e}")

    def save_checkpoints_metadata(self):
        checkpoints_file = self.library_dir / "checkpoints.txt"
        try:
            content = '\n'.join([
                f"title:{cp.book_title}|index:{cp.sentence_index}|date:{cp.saved_date.isoformat()}|path:{cp.file_path}"
                for cp in self.checkpoints
            ])
            with open(checkpoints_file, 'w', encoding='utf-8') as f:
                f.write(content)
        except Exception as e:
            print(f"Error saving checkpoints metadata: {e}")

    def load_book_from_library(self, book: Book) -> Optional[list[str]]:
        try:
            with open(book.file_path, 'r', encoding='utf-8') as f:
                return f.read().splitlines()
        except Exception as e:
            print(f"Error loading book: {e}")
            return None

    def load_checkpoint(self, checkpoint: Checkpoint) -> Optional[list[str]]:
        try:
            with open(checkpoint.file_path, 'r', encoding='utf-8') as f:
                return f.read().splitlines()
        except Exception as e:
            print(f"Error loading checkpoint: {e}")
            return None

    def delete_book(self, book: Book) -> bool:
        try:
            Path(book.file_path).unlink()
            self.library.remove(book)

            # Also remove associated checkpoints
            self.checkpoints = [cp for cp in self.checkpoints if cp.book_title != book.title]

            self.save_library_metadata()
            self.save_checkpoints_metadata()
            return True
        except Exception as e:
            print(f"Error deleting book: {e}")
            return False

    def delete_checkpoint(self, checkpoint: Checkpoint) -> bool:
        try:
            self.checkpoints.remove(checkpoint)
            self.save_checkpoints_metadata()
            return True
        except Exception as e:
            print(f"Error deleting checkpoint: {e}")
            return False
