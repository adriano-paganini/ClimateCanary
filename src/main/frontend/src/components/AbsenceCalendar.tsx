import React, { useState } from 'react';
import ReactDOM from 'react-dom';
import { Calendar, dateFnsLocalizer, View, EventProps } from 'react-big-calendar';
import { format, parse, startOfWeek, getDay } from 'date-fns';
import { enGB } from 'date-fns/locale/en-GB';
import 'react-big-calendar/lib/css/react-big-calendar.css';
import { AbsenceDTO, UserxDTO } from '../generated-skeleton-api';
import '../styles/AbsenceCalendar.css';

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

const AGENDA_WINDOW_OPTIONS = [
    { label: '1 week', value: '7' },
    { label: '2 weeks', value: '14' },
    { label: '1 month', value: '30' },
    { label: '3 months', value: '90' },
    { label: 'Custom', value: 'custom' },
];

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

const startOfDisplayWeek = (date: Date) => startOfWeek(date, { weekStartsOn: 1 });

const addDays = (date: Date, days: number) => {
    const next = new Date(date);
    next.setDate(next.getDate() + days);
    return next;
};

const addMonths = (date: Date, months: number) => {
    const next = new Date(date);
    next.setMonth(next.getMonth() + months);
    return next;
};

const daysInclusive = (start: Date, end: Date) => {
    const startMidnight = new Date(start.getFullYear(), start.getMonth(), start.getDate());
    const endMidnight = new Date(end.getFullYear(), end.getMonth(), end.getDate());
    return Math.max(1, Math.floor((endMidnight.getTime() - startMidnight.getTime()) / 86400000) + 1);
};

const isValidDate = (date: Date) => !Number.isNaN(date.getTime());

const formatDateInput = (date: Date) => isValidDate(date) ? format(date, 'yyyy-MM-dd') : '';

const dateFromInput = (value: string) => {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) {
        return null;
    }

    const date = new Date(`${value}T00:00:00`);
    return isValidDate(date) ? date : null;
};

const EventComponent = ({ event }: EventProps<CalEvent>) => {
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
    const [agendaWindow, setAgendaWindow] = useState('30');
    const [customAgendaEndDate, setCustomAgendaEndDate] = useState(addDays(new Date(), 29));

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

    const isCustomAgenda = agendaWindow === 'custom';
    const agendaLength = isCustomAgenda ? daysInclusive(currentDate, customAgendaEndDate) : Number(agendaWindow);
    const agendaEndDate = new Date(currentDate);
    agendaEndDate.setDate(agendaEndDate.getDate() + agendaLength - 1);
    const activeAgendaEndDate = isCustomAgenda ? customAgendaEndDate : agendaEndDate;

    const visibleRangeLabel = (() => {
        if (currentView === 'month') {
            return format(currentDate, 'MMMM yyyy');
        }
        if (currentView === 'week') {
            const weekStart = startOfDisplayWeek(currentDate);
            const weekEnd = addDays(weekStart, 6);
            return `${format(weekStart, 'dd.MM.yyyy')} - ${format(weekEnd, 'dd.MM.yyyy')}`;
        }
        return `${format(currentDate, 'dd.MM.yyyy')} - ${format(activeAgendaEndDate, 'dd.MM.yyyy')}`;
    })();

    const navigateBy = (direction: -1 | 1) => {
        if (currentView === 'month') {
            setCurrentDate(addMonths(currentDate, direction));
            return;
        }
        if (currentView === 'week') {
            setCurrentDate(addDays(currentDate, 7 * direction));
            return;
        }

        const days = isCustomAgenda ? daysInclusive(currentDate, activeAgendaEndDate) : agendaLength;
        const nextStart = addDays(currentDate, days * direction);
        setCurrentDate(nextStart);
        if (isCustomAgenda) {
            setCustomAgendaEndDate(addDays(activeAgendaEndDate, days * direction));
        }
    };

    const jumpToDate = (date: Date) => {
        if (isCustomAgenda) {
            const days = daysInclusive(currentDate, activeAgendaEndDate);
            setCustomAgendaEndDate(addDays(date, days - 1));
        }
        setCurrentDate(date);
    };

    return (
        <div className="absence-calendar">
            <div className="absence-calendar-header">
                <div className="absence-view-tabs" aria-label="Calendar view">
                    {(['month', 'week', 'agenda'] as View[]).map(view => (
                        <button
                            key={view}
                            type="button"
                            className={currentView === view ? 'active' : ''}
                            onClick={() => setCurrentView(view)}
                        >
                            {view === 'month' ? 'Month' : view === 'week' ? 'Week' : 'Agenda'}
                        </button>
                    ))}
                </div>

                <div className="absence-calendar-navigation">
                    <button type="button" onClick={() => navigateBy(-1)} aria-label="Previous range">
                        <i className="pi pi-chevron-left" aria-hidden="true" />
                    </button>
                    <button
                        type="button"
                        onClick={() => {
                            const today = new Date();
                            setCurrentDate(today);
                            if (isCustomAgenda) {
                                setCustomAgendaEndDate(addDays(today, agendaLength - 1));
                            }
                        }}
                    >
                        Today
                    </button>
                    <button type="button" onClick={() => navigateBy(1)} aria-label="Next range">
                        <i className="pi pi-chevron-right" aria-hidden="true" />
                    </button>
                </div>

                <div className="absence-calendar-range">{visibleRangeLabel}</div>

                <div className="absence-date-jump">
                    <label htmlFor="calendar-jump-date">Jump to</label>
                    <input
                        id="calendar-jump-date"
                        type="date"
                        min="1900-01-01"
                        max="9999-12-31"
                        value={formatDateInput(currentDate)}
                        onChange={(event) => {
                            const nextDate = dateFromInput(event.target.value);
                            if (nextDate) {
                                jumpToDate(nextDate);
                            }
                        }}
                    />
                </div>
            </div>

            <div className="absence-calendar-legend">
                {Object.entries(TYPE_LABEL).map(([key, label]) => (
                    <div key={key} className="absence-legend-item">
                        <span style={{ background: TYPE_COLOR[key] }} />
                        {label}
                    </div>
                ))}
            </div>

            {currentView === 'agenda' && (
                <div className="absence-agenda-controls">
                    <div className="absence-agenda-control">
                        <label htmlFor="agenda-start-date">Agenda starts</label>
                        <input
                            id="agenda-start-date"
                            type="date"
                            min="1900-01-01"
                            max="9999-12-31"
                            value={formatDateInput(currentDate)}
                            onChange={(event) => {
                                const nextDate = dateFromInput(event.target.value);
                                if (nextDate) {
                                    jumpToDate(nextDate);
                                }
                            }}
                        />
                    </div>
                    <div className="absence-agenda-control">
                        <label htmlFor="agenda-window">Show</label>
                        <select
                            id="agenda-window"
                            value={agendaWindow}
                            onChange={(event) => {
                                const nextWindow = event.target.value;
                                setAgendaWindow(nextWindow);
                                if (nextWindow === 'custom') {
                                    setCustomAgendaEndDate(agendaEndDate);
                                }
                            }}
                        >
                            {AGENDA_WINDOW_OPTIONS.map(option => (
                                <option key={option.value} value={option.value}>{option.label}</option>
                            ))}
                        </select>
                    </div>
                    {isCustomAgenda && (
                        <div className="absence-agenda-control">
                            <label htmlFor="agenda-end-date">Agenda ends</label>
                            <input
                                id="agenda-end-date"
                                type="date"
                                min={formatDateInput(currentDate)}
                                max="9999-12-31"
                                value={formatDateInput(customAgendaEndDate)}
                                onChange={(event) => {
                                    const nextEnd = dateFromInput(event.target.value);
                                    if (nextEnd) {
                                        setCustomAgendaEndDate(nextEnd < currentDate ? currentDate : nextEnd);
                                    }
                                }}
                            />
                        </div>
                    )}
                    <div className="absence-agenda-range">
                        {format(currentDate, 'dd.MM.yyyy')} - {format(activeAgendaEndDate, 'dd.MM.yyyy')}
                    </div>
                </div>
            )}

            <div style={{ height: '600px' }}>
                <Calendar
                    localizer={localizer}
                    events={events}
                    view={currentView}
                    onView={setCurrentView}
                    date={currentDate}
                    onNavigate={setCurrentDate}
                    views={['month', 'week', 'agenda']}
                    length={agendaLength}
                    toolbar={false}
                    eventPropGetter={eventPropGetter}
                    components={{ event: EventComponent as React.ComponentType<EventProps<CalEvent>> }}
                    messages={{
                        agenda: 'Agenda',
                        noEventsInRange: 'No absences in the selected agenda window.',
                    }}
                    popup
                    style={{ height: '100%' }}
                />
            </div>
        </div>
    );
};

export default AbsenceCalendar;
