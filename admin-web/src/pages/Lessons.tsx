import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch, getErrorMessage } from '../lib/api';

interface AdminCourseOption { id: string; title: string; }
interface AdminLesson { id: string; courseId: string | null; creatorId: string | null; title: string; theoryContent: string; }
type TheorySectionType = 'CONCEPT' | 'EXPLANATION' | 'STEPS' | 'EXAMPLE' | 'WARNING';
interface TheorySection { id: string; type: TheorySectionType; title: string | null; content: string; position: number; }
interface TheorySectionForm { id: string; type: TheorySectionType; title: string; content: string; }
interface LessonForm { title: string; theoryContent: string; isStandalone: boolean; courseId: string; sections: TheorySectionForm[]; }

const TYPES: TheorySectionType[] = ['CONCEPT', 'EXPLANATION', 'STEPS', 'EXAMPLE', 'WARNING'];
const standalone = '__standalone__';
const emptyForm: LessonForm = { title: '', theoryContent: '', isStandalone: false, courseId: '', sections: [] };
const newSection = (): TheorySectionForm => ({ id: crypto.randomUUID(), type: 'CONCEPT', title: '', content: '' });

async function fetchCourses() { return apiFetch<AdminCourseOption[]>('/admin/courses'); }
async function fetchLessons(filter: string) {
  const params = new URLSearchParams();
  if (filter === standalone) params.set('courseId', ''); else if (filter) params.set('courseId', filter);
  const query = params.toString();
  return (await apiFetch<{ items: AdminLesson[] }>(query ? `/admin/lessons?${query}` : '/admin/lessons')).items;
}
async function fetchSections(id: string) { return (await apiFetch<{ sections: TheorySection[] }>(`/admin/lessons/${id}/theory-sections`)).sections; }
async function saveSections(id: string, sections: TheorySectionForm[]) {
  await apiFetch(`/admin/lessons/${id}/theory-sections`, { method: 'PUT', body: JSON.stringify({ sections: sections.map(({ type, title, content }) => ({ type, title: title.trim() || null, content })) }) });
}
async function saveLesson(id: string | null, form: LessonForm) {
  const body = { title: form.title, theoryContent: form.theoryContent, courseId: form.isStandalone ? null : form.courseId };
  if (id) {
    await apiFetch<AdminLesson>(`/admin/lessons/${id}`, { method: 'PUT', body: JSON.stringify(body) });
    await saveSections(id, form.sections);
  } else {
    await apiFetch<AdminLesson>('/admin/lessons', { method: 'POST', body: JSON.stringify({ ...body, id: crypto.randomUUID(), theorySections: form.sections.map(({ type, title, content }) => ({ type, title: title.trim() || null, content })) }) });
  }
}

export default function Lessons() {
  const client = useQueryClient();
  const [filter, setFilter] = useState('');
  const [form, setForm] = useState<LessonForm>(emptyForm);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const coursesQuery = useQuery({ queryKey: ['admin-courses-options'], queryFn: fetchCourses });
  const lessonsQuery = useQuery({ queryKey: ['admin-lessons', filter], queryFn: () => fetchLessons(filter) });
  const save = useMutation({ mutationFn: () => saveLesson(editingId, form), onSuccess: async () => { await client.invalidateQueries({ queryKey: ['admin-lessons'] }); setForm(emptyForm); setEditingId(null); setError(null); setFeedback('Lesson saved successfully.'); }, onError: (e) => setError(getErrorMessage(e, 'Failed to save lesson.')) });
  const remove = useMutation({ mutationFn: (id: string) => apiFetch(`/admin/lessons/${id}`, { method: 'DELETE' }), onSuccess: async () => { await client.invalidateQueries({ queryKey: ['admin-lessons'] }); setFeedback('Lesson deleted successfully.'); }, onError: (e) => setError(getErrorMessage(e, 'Failed to delete lesson.')) });
  const courses = coursesQuery.data ?? [];
  const lessons = lessonsQuery.data ?? [];
  const isSaving = save.isPending;
  const patchSection = (id: string, patch: Partial<TheorySectionForm>) => setForm((current) => ({ ...current, sections: current.sections.map((item) => item.id === id ? { ...item, ...patch } : item) }));
  const move = (index: number, delta: number) => setForm((current) => { const target = index + delta; if (target < 0 || target >= current.sections.length) return current; const sections = [...current.sections]; [sections[index], sections[target]] = [sections[target], sections[index]]; return { ...current, sections }; });
  function reset() { setEditingId(null); setForm(emptyForm); setError(null); }
  async function edit(lesson: AdminLesson) {
    setEditingId(lesson.id); setFeedback(null); setError(null);
    try {
      const existing = await fetchSections(lesson.id);
      const sections = existing.length ? existing.map((item) => ({ id: item.id, type: item.type, title: item.title ?? '', content: item.content })) : lesson.theoryContent.trim() ? [{ id: crypto.randomUUID(), type: 'CONCEPT' as const, title: '', content: lesson.theoryContent }] : [];
      setForm({ title: lesson.title, theoryContent: lesson.theoryContent, isStandalone: lesson.courseId === null, courseId: lesson.courseId ?? '', sections });
    } catch (e) { setEditingId(null); setError(getErrorMessage(e, 'Failed to load theory sections.')); }
  }
  function submit(event: FormEvent) { event.preventDefault(); setError(null); setFeedback(null); if (!form.isStandalone && !form.courseId) return setError('Select a course or mark the lesson as standalone.'); if (form.sections.some((item) => !item.content.trim())) return setError('Every theory section needs content.'); save.mutate(); }
  const courseLabel = (id: string | null) => id === null ? 'Standalone' : courses.find((course) => course.id === id)?.title ?? id;
  return <div className="page"><h2>Lessons</h2>{feedback && <div className="success-banner">{feedback}</div>}{error && <div className="error-banner">{error}</div>}<div className="content-grid">
    <section className="panel"><h3>{editingId ? 'Edit lesson' : 'Create lesson'}</h3><form className="entity-form" onSubmit={submit}>
      <label>Title<input value={form.title} required disabled={isSaving} onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))} /></label>
      <fieldset><legend>Theory sections</legend><p className="hint-text">Rendered in this order. Legacy content remains a compatibility fallback.</p>
        {form.sections.map((section, index) => <div className="theory-section-editor" key={section.id}><div className="form-actions"><strong>Section {index + 1}</strong><button type="button" className="secondary-btn" disabled={isSaving || index === 0} onClick={() => move(index, -1)}>Up</button><button type="button" className="secondary-btn" disabled={isSaving || index === form.sections.length - 1} onClick={() => move(index, 1)}>Down</button><button type="button" className="danger-btn" disabled={isSaving} onClick={() => setForm((f) => ({ ...f, sections: f.sections.filter((item) => item.id !== section.id) }))}>Remove</button></div>
          <label>Type<select value={section.type} disabled={isSaving} onChange={(e) => patchSection(section.id, { type: e.target.value as TheorySectionType })}>{TYPES.map((type) => <option key={type}>{type}</option>)}</select></label><label>Optional title<input value={section.title} maxLength={160} disabled={isSaving} onChange={(e) => patchSection(section.id, { title: e.target.value })} /></label><label>Content<textarea value={section.content} rows={5} required disabled={isSaving} onChange={(e) => patchSection(section.id, { content: e.target.value })} /></label>
        </div>)}<button type="button" className="secondary-btn" disabled={isSaving} onClick={() => setForm((f) => ({ ...f, sections: [...f.sections, newSection()] }))}>Add section</button></fieldset>
      <label>Legacy theory fallback<textarea value={form.theoryContent} rows={4} disabled={isSaving} onChange={(e) => setForm((f) => ({ ...f, theoryContent: e.target.value }))} /></label>
      <label className="checkbox-row"><input type="checkbox" checked={form.isStandalone} disabled={isSaving} onChange={(e) => setForm((f) => ({ ...f, isStandalone: e.target.checked, courseId: e.target.checked ? '' : f.courseId }))} />Standalone lesson</label><label>Assigned course<select value={form.courseId} disabled={form.isStandalone || isSaving || coursesQuery.isLoading} onChange={(e) => setForm((f) => ({ ...f, courseId: e.target.value }))}><option value="">Select a course</option>{courses.map((course) => <option key={course.id} value={course.id}>{course.title}</option>)}</select></label><div className="form-actions"><button disabled={isSaving}>{editingId ? 'Save changes' : 'Create lesson'}</button>{editingId && <button type="button" className="secondary-btn" onClick={reset}>Cancel</button>}</div>
    </form></section>
    <section className="panel"><div className="panel-header"><h3>Lesson list</h3><label>Filter<select value={filter} onChange={(e) => setFilter(e.target.value)}><option value="">All lessons</option><option value={standalone}>Standalone only</option>{courses.map((course) => <option key={course.id} value={course.id}>{course.title}</option>)}</select></label></div>{lessonsQuery.isLoading && <p>Loading lessons...</p>}{lessonsQuery.isError && <p className="error-text">Error: {(lessonsQuery.error as Error).message}</p>}{lessonsQuery.data && <table className="data-table"><thead><tr><th>ID</th><th>Title</th><th>Assignment</th><th>Owner</th><th>Theory fallback</th><th>Actions</th></tr></thead><tbody>{lessons.length ? lessons.map((lesson) => <tr key={lesson.id}><td className="id-cell">{lesson.id}</td><td>{lesson.title}</td><td>{courseLabel(lesson.courseId)}</td><td className="id-cell">{lesson.creatorId ?? '—'}</td><td className="desc-cell">{lesson.theoryContent}</td><td><div className="table-actions"><button type="button" className="secondary-btn" onClick={() => void edit(lesson)}>Edit</button><button type="button" className="danger-btn" disabled={remove.isPending} onClick={() => { if (window.confirm(`Delete lesson "${lesson.title}" and its exercises?`)) remove.mutate(lesson.id); }}>Delete</button></div></td></tr>) : <tr><td colSpan={6} className="empty-row">No lessons found.</td></tr>}</tbody></table>}</section>
  </div></div>;
}
