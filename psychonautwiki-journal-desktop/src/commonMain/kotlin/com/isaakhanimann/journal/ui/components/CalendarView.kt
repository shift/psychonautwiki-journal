package com.isaakhanimann.journal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.data.experience.ExperienceSummary
import kotlinx.datetime.*
import kotlinx.datetime.format.*

@Composable
fun ExperienceCalendarView(
    experiences: List<ExperienceSummary>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault())) }
    
    // Group experiences by date
    val experiencesByDate = remember(experiences) {
        experiences.groupBy { 
            it.experience.sortDate.toLocalDateTime(TimeZone.currentSystemDefault()).date
        }
    }
    
    Column(modifier = modifier) {
        // Calendar header
        CalendarHeader(
            currentMonth = currentMonth,
            onPreviousMonth = { 
                currentMonth = currentMonth.minus(1, DateTimeUnit.MONTH)
                onMonthChanged(currentMonth)
            },
            onNextMonth = { 
                currentMonth = currentMonth.plus(1, DateTimeUnit.MONTH)
                onMonthChanged(currentMonth)
            },
            onToday = {
                currentMonth = Clock.System.todayIn(TimeZone.currentSystemDefault())
                onMonthChanged(currentMonth)
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Calendar grid
        CalendarGrid(
            currentMonth = currentMonth,
            experiencesByDate = experiencesByDate,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected
        )
    }
}

@Composable
private fun CalendarHeader(
    currentMonth: LocalDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous month"
                )
            }
            
            Text(
                text = currentMonth.format(LocalDate.Format {
                    monthName(MonthNames.ENGLISH_FULL)
                    chars(" ")
                    year()
                }),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
            
            IconButton(onClick = onNextMonth) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next month"
                )
            }
        }
        
        OutlinedButton(
            onClick = onToday,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Today")
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: LocalDate,
    experiencesByDate: Map<LocalDate, List<ExperienceSummary>>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    // Calculate calendar grid
    val firstDayOfMonth = LocalDate(currentMonth.year, currentMonth.month, 1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.ordinal // Monday = 0
    val daysInMonth = when (currentMonth.month) {
        Month.FEBRUARY -> if (currentMonth.year % 4 == 0 && (currentMonth.year % 100 != 0 || currentMonth.year % 400 == 0)) 29 else 28
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        else -> 31
    }
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    
    // Create list of dates to display (including previous/next month padding)
    val calendarDates = mutableListOf<CalendarDay>()
    
    // Add padding days from previous month
    val prevMonth = currentMonth.minus(1, DateTimeUnit.MONTH)
    val daysInPrevMonth = when (prevMonth.month) {
        Month.FEBRUARY -> if (prevMonth.year % 4 == 0 && (prevMonth.year % 100 != 0 || prevMonth.year % 400 == 0)) 29 else 28
        Month.APRIL, Month.JUNE, Month.SEPTEMBER, Month.NOVEMBER -> 30
        else -> 31
    }
    for (i in firstDayOfWeek - 1 downTo 0) {
        val date = LocalDate(prevMonth.year, prevMonth.month, daysInPrevMonth - i)
        calendarDates.add(CalendarDay(date, isCurrentMonth = false))
    }
    
    // Add days of current month
    for (day in 1..daysInMonth) {
        val date = LocalDate(currentMonth.year, currentMonth.month, day)
        calendarDates.add(CalendarDay(date, isCurrentMonth = true))
    }
    
    // Add padding days from next month
    val nextMonth = currentMonth.plus(1, DateTimeUnit.MONTH)
    var nextMonthDay = 1
    while (calendarDates.size % 7 != 0) {
        val date = LocalDate(nextMonth.year, nextMonth.month, nextMonthDay++)
        calendarDates.add(CalendarDay(date, isCurrentMonth = false))
    }
    
    Column {
        // Day names header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            dayNames.forEach { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(calendarDates) { calendarDay ->
                CalendarDayCell(
                    calendarDay = calendarDay,
                    experiences = experiencesByDate[calendarDay.date] ?: emptyList(),
                    isSelected = selectedDate == calendarDay.date,
                    isToday = today == calendarDay.date,
                    onClick = { onDateSelected(calendarDay.date) }
                )
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    calendarDay: CalendarDay,
    experiences: List<ExperienceSummary>,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val hasExperiences = experiences.isNotEmpty()
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = calendarDay.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    !calendarDay.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            
            if (hasExperiences) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun ExperienceTimelineView(
    experiences: List<ExperienceSummary>,
    selectedDate: LocalDate?,
    modifier: Modifier = Modifier
) {
    val filteredExperiences = remember(experiences, selectedDate) {
        if (selectedDate != null) {
            experiences.filter { 
                it.experience.sortDate.toLocalDateTime(TimeZone.currentSystemDefault()).date == selectedDate
            }
        } else {
            experiences
        }
    }
    
    if (filteredExperiences.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = if (selectedDate != null) {
                        "No experiences on ${selectedDate.format(LocalDate.Formats.ISO)}"
                    } else {
                        "Select a date to view experiences"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedDate != null) {
                Text(
                    text = "Experiences on ${selectedDate.format(LocalDate.Format {
                        dayOfMonth()
                        chars(" ")
                        monthName(MonthNames.ENGLISH_FULL)
                        chars(" ")
                        year()
                    })}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            filteredExperiences.forEach { experienceSummary ->
                ExperienceTimelineCard(
                    experienceSummary = experienceSummary
                )
            }
        }
    }
}

@Composable
private fun ExperienceTimelineCard(
    experienceSummary: ExperienceSummary
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = experienceSummary.experience.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                if (experienceSummary.experience.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            if (experienceSummary.experience.text.isNotBlank()) {
                Text(
                    text = experienceSummary.experience.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (experienceSummary.substanceCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${experienceSummary.substanceCount} substance${if (experienceSummary.substanceCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (experienceSummary.ingestionCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Medication,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${experienceSummary.ingestionCount} ingestion${if (experienceSummary.ingestionCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean
)