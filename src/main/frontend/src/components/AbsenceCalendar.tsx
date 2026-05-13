import React, { useState } from 'react';
import { Calendar, dateFnsLocalizer, View } from 'react-big-calendar';
import { format, parse, startOfWeek, getDay } from 'date-fns';
import { enGB } from 'date-fns/locale/en-GB';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import { AbsenceDTO, UserxDTO } from '../generated-skeleton-api';

const localizer = dateFnsLocalizer({
    format,
    parse,
    startOfWeek: (date: Date) => startOfWeek(date, { weekStartsOn: 1 }),
    getDay,
    locales: { 'en-GB': enGB },
});

const TYPE_COLOR: Record<string, string> = {
    HOLIDAY:       '#f59e0b',
    SICKNESS:      '#ef4444',
    PARENTAL_LEAVE:'#10b981',
    OTHER:         '#8b5cf6',
};

const TYPE_LABEL: Record<string, string> = {
    HOLIDAY:       'Holiday',
    SICKNESS:      'Sick Leave',
    PARENTAL_LEAVE:'Parental Leave',
    OTHER:         'Other',
};

interface CalEvent {
    id: number;
    title: string;
    start: Date;
    end: Date;
    type: string;
}

interface Props {
    absences: AbsenceDTO[];
    userMap?: Record<number, UserxDTO>;
}

const AbsenceCalendar: React.FC<Props> = ({ absences, userMap }) => {
    const [currentView, setCurrentView] = useState<View>('month');
    const [currentDate, setCurrentDate] = useState(new Date());

    const events: CalEvent[] = absences
        .filter(a => a.startDate && a.endDate)
        .map(a => {
            const start = new Date(a.startDate!);
            const end   = new Date(a.endDate!);
            const typeLabel = TYPE_LABEL[a.absenceType ?? ''] ?? (a.absenceType ?? 'Absence');

            let title = typeLabel;
            if (userMap && a.userxId !== undefined) {
                const u = userMap[a.userxId];
                const name = u
                    ? (`${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || u.username || `User ${a.userxId}`)
                    : `User ${a.userxId}`;
                title = `${name} — ${typeLabel}`;
            }
            return { id: a.id!, title, start, end, type: a.absenceType ?? '' };
        });

    const eventPropGetter = (event: CalEvent) => ({
        style: {
            backgroundColor: TYPE_COLOR[event.type] ?? '#6b7280',
            border: 'none',
            borderRadius: '4px',
            color: '#fff',
            fontSize: '0.8rem',
            padding: '2px 6px',
        },
    });

    return (
        <div>
            {/* Legend */}
            <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', marginBottom: '1rem' }}>
                {Object.entries(TYPE_LABEL).map(([key, label]) => (
                    <div key={key} style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.85rem', color: '#374151' }}>
                        <span style={{ width: '12px', height: '12px', borderRadius: '3px', background: TYPE_COLOR[key], display: 'inline-block' }} />
                        {label}
                    </div>
                ))}
            </div>

            <div style={{ height: '600px' }}>
                <Calendar
                    localizer={localizer}
                    events={events}
                    view={currentView}
                    onView={setCurrentView}
                    date={currentDate}
                    onNavigate={setCurrentDate}
                    views={['month', 'week', 'agenda']}
                    eventPropGetter={eventPropGetter}
                    popup
                    style={{ height: '100%' }}
                />
            </div>
        </div>
    );
};

export default AbsenceCalendar;
