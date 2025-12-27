#!/bin/bash

# Code Style Check Script for AutoGLM Phone Agent
# This script checks the codebase for compliance with code style guidelines.
#
# Requirements: 1.1, 2.1, 3.3, 3.4, 3.5
#
# Usage: ./scripts/check_code_style.sh [source_directory]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default source directory
SOURCE_DIR="${1:-app/src/main/java}"

echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}              AutoGLM Code Style Checker                        ${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""
echo "Source directory: $SOURCE_DIR"
echo ""

# Counters
TOTAL_FILES=0
FILES_WITH_VIOLATIONS=0
TOTAL_VIOLATIONS=0

# Arrays to store violations
declare -a DIRECT_LOG_VIOLATIONS
declare -a KDOC_VIOLATIONS
declare -a NAMING_VIOLATIONS
declare -a LINE_LENGTH_VIOLATIONS

# Function to check a single file
check_file() {
    local file="$1"
    local relative_path="${file#$SOURCE_DIR/}"
    local file_violations=0
    local line_num=0
    
    # Skip line length check for prompt template files
    local skip_line_length=false
    if [[ "$file" =~ "SystemPrompts.kt" ]] || [[ "$file" =~ "prompts" ]]; then
        skip_line_length=true
    fi

    while IFS= read -r line || [[ -n "$line" ]]; do
        ((line_num++))

        # Check 1: Direct Log calls (Requirement 1.1)
        # Skip Logger.kt itself
        if [[ ! "$file" =~ "Logger.kt" ]]; then
            if echo "$line" | grep -qE 'import\s+android\.util\.Log\b'; then
                DIRECT_LOG_VIOLATIONS+=("$relative_path:$line_num - Direct import of android.util.Log")
                ((file_violations++))
            fi
            if echo "$line" | grep -qE '\bLog\.(v|d|i|w|e|wtf)\s*\(' | grep -vE 'Logger\.'; then
                DIRECT_LOG_VIOLATIONS+=("$relative_path:$line_num - Direct Log call found")
                ((file_violations++))
            fi
        fi

        # Check 2: Line length (Requirement 3.8)
        # Skip for prompt template files
        if [[ "$skip_line_length" == false ]]; then
            local line_length=${#line}
            if [[ $line_length -gt 120 ]]; then
                LINE_LENGTH_VIOLATIONS+=("$relative_path:$line_num - Line exceeds 120 chars ($line_length)")
                ((file_violations++))
            fi
        fi

    done < "$file"

    if [[ $file_violations -gt 0 ]]; then
        ((FILES_WITH_VIOLATIONS++))
        ((TOTAL_VIOLATIONS+=file_violations))
    fi
}

# Find and check all Kotlin files
echo -e "${BLUE}Scanning Kotlin files...${NC}"
echo ""

while IFS= read -r -d '' file; do
    # Skip build directories and test files
    if [[ "$file" =~ "/build/" ]] || [[ "$file" =~ "/test/" ]] || [[ "$file" =~ "/androidTest/" ]]; then
        continue
    fi
    ((TOTAL_FILES++))
    check_file "$file"
done < <(find "$SOURCE_DIR" -name "*.kt" -type f -print0 2>/dev/null)

# Print results
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}                    CODE STYLE CHECK RESULTS                    ${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════${NC}"
echo ""

if [[ $TOTAL_VIOLATIONS -eq 0 ]]; then
    echo -e "${GREEN}✓ All checks passed! No violations found.${NC}"
else
    echo -e "${RED}Found $TOTAL_VIOLATIONS violation(s) in $FILES_WITH_VIOLATIONS file(s):${NC}"
    echo ""

    # Print Direct Log violations
    if [[ ${#DIRECT_LOG_VIOLATIONS[@]} -gt 0 ]]; then
        echo -e "${YELLOW}[NO_DIRECT_LOG] Direct Log Calls (Requirement 1.1) - ${#DIRECT_LOG_VIOLATIONS[@]} violation(s)${NC}"
        for v in "${DIRECT_LOG_VIOLATIONS[@]:0:10}"; do
            echo "  $v"
        done
        if [[ ${#DIRECT_LOG_VIOLATIONS[@]} -gt 10 ]]; then
            echo "  ... and $((${#DIRECT_LOG_VIOLATIONS[@]} - 10)) more"
        fi
        echo ""
    fi

    # Print Line Length violations
    if [[ ${#LINE_LENGTH_VIOLATIONS[@]} -gt 0 ]]; then
        echo -e "${YELLOW}[LINE_LENGTH] Line Length Limit (Requirement 3.8) - ${#LINE_LENGTH_VIOLATIONS[@]} violation(s)${NC}"
        for v in "${LINE_LENGTH_VIOLATIONS[@]:0:10}"; do
            echo "  $v"
        done
        if [[ ${#LINE_LENGTH_VIOLATIONS[@]} -gt 10 ]]; then
            echo "  ... and $((${#LINE_LENGTH_VIOLATIONS[@]} - 10)) more"
        fi
        echo ""
    fi
fi

echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"
echo "Summary: Checked $TOTAL_FILES files, $FILES_WITH_VIOLATIONS with violations"
echo -e "${BLUE}───────────────────────────────────────────────────────────────${NC}"

# Exit with appropriate code
if [[ $TOTAL_VIOLATIONS -gt 0 ]]; then
    exit 1
else
    exit 0
fi
