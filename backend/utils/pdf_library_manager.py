import sqlite3
import json
from datetime import datetime
from typing import List, Dict, Optional

class PDFLibraryManager:
    def __init__(self, db_path: str = 'pdf_library.db'):
        '''Initialize the PDF Library Manager with SQLite database'''
        self.db_path = db_path
        self._init_database()
    
    def _init_database(self):
        '''Initialize the database with required tables'''
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute('''
                CREATE TABLE IF NOT EXISTS pdf_library (
                    id             INTEGER PRIMARY KEY AUTOINCREMENT,
                    filename       TEXT NOT NULL,
                    sentences      TEXT NOT NULL,
                    date_added     TIMESTAMP NOT NULL,
                    sentence_count INTEGER NOT NULL
                )
            ''')
            conn.commit()
        
        return;
    
    def pdf_exists(self, filename: str) -> bool:
        '''
        Check if a PDF with the given filename already exists in the library
        
        Args:
            filename: Name of the PDF file to check
            
        Returns:
            bool: True if PDF exists, False otherwise
        '''
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute('SELECT COUNT(*) FROM pdf_library WHERE filename = ?', (filename,))
            count  = cursor.fetchone()[0]

            return count > 0;
    
    def add_pdf(self, filename: str, sentences: List[str]) -> int:
        '''
        Add a new PDF record to the library (only if it doesn't already exist)
        
        Args:
            filename:  Name of the PDF file
            sentences: List of processed sentences from the PDF
            
        Returns:
            int: ID of the newly created record, or existing record ID if already exists
        '''
        # Check if PDF already exists
        if self.pdf_exists(filename):
            # Return the existing PDF's ID
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                cursor.execute('SELECT id FROM pdf_library WHERE filename = ?', (filename,))

                return cursor.fetchone()[0];
        
        # Add new PDF if it doesn't exist
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            
            # Convert sentences list to JSON string for storage
            sentences_json = json.dumps(sentences)
            current_time   = datetime.now()
            sentence_count = len(sentences)
            
            cursor.execute('''
                INSERT INTO pdf_library (filename, sentences, date_added, sentence_count)
                VALUES (?, ?, ?, ?)
            ''', (filename, sentences_json, current_time, sentence_count))
            
            conn.commit()

            return cursor.lastrowid;
    
    def get_all_pdfs(self) -> List[Dict]:
        '''
        Get all PDF records from the library
        
        Returns:
            List of dictionaries containing PDF information
        '''
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute('''
                SELECT   id, filename, date_added, sentence_count
                FROM     pdf_library
                ORDER BY date_added DESC
            ''')
            
            rows = cursor.fetchall()
            
            pdfs = []
            for row in rows:
                pdfs.append({
                    'id':             row[0],
                    'filename':       row[1],
                    'date_added':     row[2],
                    'sentence_count': row[3]
                })
            
            return pdfs;
    
    def get_pdf_by_id(self, pdf_id: int) -> Optional[Dict]:
        '''
        Get a specific PDF record by ID
        
        Args:
            pdf_id: ID of the PDF record
            
        Returns:
            Dictionary containing complete PDF information or None if not found
        '''
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute('''
                SELECT id, filename, sentences, date_added, sentence_count
                FROM   pdf_library
                WHERE  id = ?
            ''', (pdf_id,))
            
            row = cursor.fetchone()
            
            if row:
                # Convert JSON string back to list
                sentences = json.loads(row[2])
                
                return {
                    'id':             row[0],
                    'filename':       row[1],
                    'sentences':      sentences,
                    'date_added':     row[3],
                    'sentence_count': row[4]
                };
            
            return None;
    
    def delete_pdf(self, pdf_id: int) -> bool:
        '''
        Delete a PDF record from the library
        
        Args:
            pdf_id: ID of the PDF record to delete
            
        Returns:
            bool: True if deletion was successful, False otherwise
        '''
        with sqlite3.connect(self.db_path) as conn:
            cursor = conn.cursor()
            cursor.execute('DELETE FROM pdf_library WHERE id = ?', (pdf_id,))
            conn.commit()
            
            return cursor.rowcount > 0;
