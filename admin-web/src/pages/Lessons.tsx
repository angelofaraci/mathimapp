import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch, getErrorMessage } from '../lib/api';

interface AdminCourseOption {
  id: string;
  title: string;
}

interface AdminLesson {
  id: string;
  courseId: string | null;
  creatorId: string | null;
  title: string;
  theoryContent: string;
}

interface AdminLessonListResponse {
  items: AdminLesson[];
}

interface LessonFormState {
  title: string;
  theoryContent: string;
  isStandalone: boolean;
  courseId: string;
}

const emptyForm: LessonFormState = {
  title: '',
  theoryContent: '',
  isStandalone: false,
  courseId: '',
};

const standaloneFilterValue = '__standalone__';

async function fetchCourses(): Promise<AdminCourseOption[]> {
  return apiFetch<AdminCourseOption[]>('/admin/courses');
}

async function fetchLessons(filterValue: string): Promise<AdminLesson[]> {
  const params = new URLSearchParams();

  if (filterValue === standaloneFilterValue) {
    params.set('courseId', '');
  } else if (filterValue) {
    params.set('courseId', filterValue);
  }

  const query = params.toString();
  const path = query ? `/admin/lessons?${query}` : '/admin/lessons';
  const response = await apiFetch<AdminLessonListResponse>(path);
  return response.items;
}

async function createLesson(payload: LessonFormState): Promise<void> {
  await apiFetch('/admin/lessons', {
    method: 'POST',
    body: JSON.stringify({
      id: crypto.randomUUID(),
      title: payload.title,
      theoryContent: payload.theoryContent,
      courseId: payload.isStandalone ? null : payload.courseId,
    }),
  });
}

async function updateLesson(
  lessonId: string,
  payload: LessonFormState,
): Promise<void> {
  await apiFetch(`/admin/lessons/${lessonId}`, {
    method: 'PUT',
    body: JSON.stringify({
      title: payload.title,
      theoryContent: payload.theoryContent,
      courseId: payload.isStandalone ? null : payload.courseId,
    }),
  });
}

async function deleteLesson(lessonId: string): Promise<void> {
  await apiFetch(`/admin/lessons/${lessonId}`, {
    method: 'DELETE',
  });
}

export default function Lessons() {
  const queryClient = useQueryClient();
  const [filterValue, setFilterValue] = useState('');
  const [form, setForm] = useState<LessonFormState>(emptyForm);
  const [editingLessonId, setEditingLessonId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorBanner, setErrorBanner] = useState<string | null>(null);

  const coursesQuery = useQuery({
    queryKey: ['admin-courses-options'],
    queryFn: fetchCourses,
  });

  const lessonsQuery = useQuery({
    queryKey: ['admin-lessons', filterValue],
    queryFn: () => fetchLessons(filterValue),
  });

  const createMutation = useMutation({
    mutationFn: createLesson,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin-lessons'] });
      setForm(emptyForm);
      setFeedback('Lesson created successfully.');
      setErrorBanner(null);
    },
    onError: (mutationError) => {
      setErrorBanner(getErrorMessage(mutationError, 'Failed to create lesson.'));
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ lessonId, payload }: { lessonId: string; payload: LessonFormState }) =>
      updateLesson(lessonId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin-lessons'] });
      setForm(emptyForm);
      setEditingLessonId(null);
      setFeedback('Lesson updated successfully.');
      setErrorBanner(null);
    },
    onError: (mutationError) => {
      setErrorBanner(getErrorMessage(mutationError, 'Failed to update lesson.'));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteLesson,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin-lessons'] });
      setFeedback('Lesson deleted successfully.');
      setErrorBanner(null);
    },
    onError: (mutationError) => {
      setErrorBanner(getErrorMessage(mutationError, 'Failed to delete lesson.'));
    },
  });

  const courseOptions = coursesQuery.data ?? [];
  const lessons = lessonsQuery.data ?? [];
  const isSaving = createMutation.isPending || updateMutation.isPending;

  function resetForm() {
    setForm(emptyForm);
    setEditingLessonId(null);
    setErrorBanner(null);
  }

  function startEditing(lesson: AdminLesson) {
    setForm({
      title: lesson.title,
      theoryContent: lesson.theoryContent,
      isStandalone: lesson.courseId === null,
      courseId: lesson.courseId ?? '',
    });
    setEditingLessonId(lesson.id);
    setFeedback(null);
    setErrorBanner(null);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFeedback(null);
    setErrorBanner(null);

    if (!form.isStandalone && !form.courseId) {
      setErrorBanner('Select a course or mark the lesson as standalone.');
      return;
    }

    if (editingLessonId) {
      updateMutation.mutate({ lessonId: editingLessonId, payload: form });
      return;
    }

    createMutation.mutate(form);
  }

  function handleDelete(lesson: AdminLesson) {
    if (!window.confirm(`Delete lesson "${lesson.title}" and its exercises?`)) {
      return;
    }

    setFeedback(null);
    setErrorBanner(null);
    deleteMutation.mutate(lesson.id);
  }

  function courseLabel(courseId: string | null): string {
    if (courseId === null) {
      return 'Standalone';
    }

    return courseOptions.find((course) => course.id === courseId)?.title ?? courseId;
  }

  return (
    <div className="page">
      <h2>Lessons</h2>

      {feedback && <div className="success-banner">{feedback}</div>}
      {errorBanner && <div className="error-banner">{errorBanner}</div>}

      <div className="content-grid">
        <section className="panel">
          <h3>{editingLessonId ? 'Edit lesson' : 'Create lesson'}</h3>
          <form className="entity-form" onSubmit={handleSubmit}>
            <label>
              Title
              <input
                type="text"
                value={form.title}
                onChange={(event) =>
                  setForm((current) => ({ ...current, title: event.target.value }))
                }
                required
                disabled={isSaving}
              />
            </label>

            <label>
              Theory content
              <textarea
                value={form.theoryContent}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    theoryContent: event.target.value,
                  }))
                }
                rows={6}
                required
                disabled={isSaving}
              />
            </label>

            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={form.isStandalone}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    isStandalone: event.target.checked,
                    courseId: event.target.checked ? '' : current.courseId,
                  }))
                }
                disabled={isSaving}
              />
              Standalone lesson
            </label>

            <label>
              Assigned course
              <select
                value={form.courseId}
                onChange={(event) =>
                  setForm((current) => ({ ...current, courseId: event.target.value }))
                }
                disabled={form.isStandalone || isSaving || coursesQuery.isLoading}
              >
                <option value="">Select a course</option>
                {courseOptions.map((course) => (
                  <option key={course.id} value={course.id}>
                    {course.title}
                  </option>
                ))}
              </select>
            </label>

            <div className="form-actions">
              <button type="submit" disabled={isSaving}>
                {editingLessonId ? 'Save changes' : 'Create lesson'}
              </button>
              {editingLessonId && (
                <button type="button" className="secondary-btn" onClick={resetForm}>
                  Cancel
                </button>
              )}
            </div>
          </form>
        </section>

        <section className="panel">
          <div className="panel-header">
            <h3>Lesson list</h3>
            <label>
              Filter
              <select
                value={filterValue}
                onChange={(event) => setFilterValue(event.target.value)}
              >
                <option value="">All lessons</option>
                <option value={standaloneFilterValue}>Standalone only</option>
                {courseOptions.map((course) => (
                  <option key={course.id} value={course.id}>
                    {course.title}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {lessonsQuery.isLoading && <p>Loading lessons...</p>}
          {lessonsQuery.isError && (
            <p className="error-text">Error: {(lessonsQuery.error as Error).message}</p>
          )}

          {lessonsQuery.data && (
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Title</th>
                  <th>Assignment</th>
                  <th>Owner</th>
                  <th>Theory</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {lessons.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="empty-row">
                      No lessons found.
                    </td>
                  </tr>
                ) : (
                  lessons.map((lesson) => (
                    <tr key={lesson.id}>
                      <td className="id-cell">{lesson.id}</td>
                      <td>{lesson.title}</td>
                      <td>{courseLabel(lesson.courseId)}</td>
                      <td className="id-cell">{lesson.creatorId ?? '—'}</td>
                      <td className="desc-cell">{lesson.theoryContent}</td>
                      <td>
                        <div className="table-actions">
                          <button type="button" className="secondary-btn" onClick={() => startEditing(lesson)}>
                            Edit
                          </button>
                          <button
                            type="button"
                            className="danger-btn"
                            onClick={() => handleDelete(lesson)}
                            disabled={deleteMutation.isPending}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
        </section>
      </div>
    </div>
  );
}
