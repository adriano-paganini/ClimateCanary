import React, { useState } from 'react';
import ReactDOM from 'react-dom';
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

const STATUS_LABEL: Record<string, string> = {
    APPROVED: 'Approved',
    PLANNED:  'Planned',
    REJECTED: 'Rejected',
    CANCELLED:'Cancelled',
};

interface CalEvent {
    id: number;
    title: string;
    start: Date;
    end: Date;
    type: string;
    status: string;
}

interface Props {
    absences: AbsenceDTO[];
    userMap?: Record<number, UserxDTO>;
}

const EventComponent = ({ event }: { event: CalEvent }) => {
    const [pos, setPos] = useState<{ x: number; y: number } | null>(null);

    return (
        <span
            style={{ display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
            onMouseEnter={(e) => setPos({ x: e.clientX, y: e.clientY })}
            onMouseMove={(e) => setPos({ x: e.clientX, y: e.clientY })}
            onMouseLeave={() => setPos(null)}
        >
            {event.title}
            {pos && ReactDOM.createPortal(
                <div style={{
                    position: 'fixed',
                    left: pos.x + 12,
                    top: pos.y - 36,
                    backgroundColor: '#1e293b',
                    color: '#fff',
                    padding: '4px 10px',
                    borderRadius: '4px',
                    fontSize: '0.75rem',
                    fontWeight: 500,
                    pointerEvents: 'none',
                    zIndex: 9999,
                    boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
                    whiteSpace: 'nowrap',
                }}>
                    {STATUS_LABEL[event.status] ?? event.status}
                </div>,
                document.body
            )}
        </span>
    );
};

const AbsenceCalendar: React.FC<Props> = ({ absences, userMap }) => {
    const [currentView, setCurrentView] = useState<View>('month');
    const [currentDate, setCurrentDate] = useState(new Date());

    const events: CalEvent[] = absences
        .filter(a => a.startDate && a.endDate && a.absenceStatus !== 'CANCELLED')
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
            return { id: a.id!, title, start, end, type: a.absenceType ?? '', status: a.absenceStatus ?? '' };
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
                    components={{ event: EventComponent as React.ComponentType<object> }}
                    popup
                    style={{ height: '100%' }}
                />
            </div>
        </div>
    );
};

export default AbsenceCalendar;
