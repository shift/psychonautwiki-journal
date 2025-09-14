#!/bin/bash

# Test database functionality
echo "Testing database functionality..."

# Check if database file exists and is readable
DB_PATH="$HOME/.psychonautwiki-journal/database.db"
if [ -f "$DB_PATH" ]; then
    echo "✓ Database file exists at: $DB_PATH"
    echo "  File size: $(du -h "$DB_PATH" | cut -f1)"
else
    echo "✗ Database file not found at: $DB_PATH"
    exit 1
fi

# Use sqlite3 to check database structure
echo
echo "Database Tables:"
sqlite3 "$DB_PATH" ".tables"

echo
echo "Experience table schema:"
sqlite3 "$DB_PATH" ".schema Experience"

echo
echo "Checking for existing data:"
echo "Experience count: $(sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM Experience;")"
echo "Ingestion count: $(sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM Ingestion;")"

echo
echo "Sample experiences (if any):"
sqlite3 "$DB_PATH" "SELECT id, title, creationDate FROM Experience LIMIT 3;" | head -3

echo
echo "Database test completed."