import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { apiFetch, getErrorMessage } from '../lib/api';

// ---- Types (mirror AdminCourseResponse) ----

interface AdminCourse {
  id: string;
  title: string;
  description: string;
  creatorId: string;
  creatorName: string;
  enrollmentCount: number;
  isOfficial: boolean;
  schoolYear: number;
}

interface CourseFormState {
  title: string;
  description: string;
  isOfficial: boolean;
  schoolYear: number;
}

const emptyForm: CourseFormState = {
  title: '',
  description: '',
  isOfficial: false,
  schoolYear: 0,
};

// ---- API call ----

async function fetchCourses(): Promise<AdminCourse[]> {
  return apiFetch<AdminCourse[]>('/admin/courses');
}

async function createCourse(payload: CourseFormState): Promise<void> {
  await apiFetch('/admin/courses', {
    method: 'POST',
    body: JSON.stringify({
      id: crypto.randomUUID(),
      ...payload,
    }),
  });
}

async function updateCourse(
  courseId: string,
  payload: CourseFormState,
): Promise<void> {
  await apiFetch(`/admin/courses/${courseId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

async function deleteCourse(courseId: string): Promise<void> {
  await apiFetch(`/admin/courses/${courseId}`, {
    method: 'DELETE',
  });
}

// ---- Component ----

export default function Courses() {
  const queryClient = useQueryClient();
  const [form, setForm] = useState<CourseFormState>(emptyForm);
  const [editingCourseId, setEditingCourseId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [errorBanner, setErrorBanner] = useState<string | null>(null);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['admin-courses'],
    queryFn: fetchCourses,
  });

  const createMutation = useMutation({
    mutationFn: createCourse,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin-courses'] });
      setForm(emptyForm);
      setFeedback('Course created successfully.');
      setErrorBanner(null);
    },
    onError: (mutationError) => {
      setErrorBanner(getErrorMessage(mutationError, 'Failed to create course.'));
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ courseId, payload }: { courseId: string; payload: CourseFormState }) =>
      updateCourse(courseId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin-courses'] });
      setForm(emptyForm);
      setEditingCourseId(null);
      setFeedback('Course updated successfully.');
      setErrorBanner(null);
    },
    onError: (mutationError) => {
      setErrorBanner(getErrorMessage(mutationError, 'Failed to update course.'));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: deleteCourse,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['admin-courses'] });
      setFeedback('Course deleted successfully.');
      setErrorBanner(null);
      if (editingCourseId) {
        setForm(emptyForm);
        setEditingCourseId(null);
      }
    },
    onError: (mutationError) => {
      setErrorBanner(getErrorMessage(mutationError, 'Failed to delete course.'));
    },
  });

  const isSaving = createMutation.isPending || updateMutation.isPending;

  function resetForm() {
    setForm(emptyForm);
    setEditingCourseId(null);
    setErrorBanner(null);
  }

  function startEditing(course: AdminCourse) {
    setForm({
      title: course.title,
      description: course.description,
      isOfficial: course.isOfficial,
      schoolYear: course.schoolYear,
    });
    setEditingCourseId(course.id);
    setFeedback(null);
    setErrorBanner(null);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFeedback(null);
    setErrorBanner(null);

    if (editingCourseId) {
      updateMutation.mutate({ courseId: editingCourseId, payload: form });
      return;
    }

    createMutation.mutate(form);
  }

  function handleDelete(course: AdminCourse) {
    if (!window.confirm(`Delete course "${course.title}"? This also removes linked lessons and exercises.`)) {
      return;
    }

    setFeedback(null);
    setErrorBanner(null);
    deleteMutation.mutate(course.id);
  }

  return (
    <div className="page">
      <h2>Courses</h2>

      {feedback && <div className="success-banner">{feedback}</div>}
      {errorBanner && <div className="error-banner">{errorBanner}</div>}

      <div className="content-grid">
        <section className="panel">
          <h3>{editingCourseId ? 'Edit course' : 'Create course'}</h3>
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
              Description
              <textarea
                value={form.description}
                onChange={(event) =>
                  setForm((current) => ({ ...current, description: event.target.value }))
                }
                rows={4}
                required
                disabled={isSaving}
              />
            </label>

            <label>
              School year
              <input
                type="number"
                min={0}
                value={form.schoolYear}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    schoolYear: Number(event.target.value) || 0,
                  }))
                }
                disabled={isSaving}
              />
            </label>

            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={form.isOfficial}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    isOfficial: event.target.checked,
                  }))
                }
                disabled={isSaving}
              />
              Official course
            </label>

            <div className="form-actions">
              <button type="submit" disabled={isSaving}>
                {editingCourseId ? 'Save changes' : 'Create course'}
              </button>
              {editingCourseId && (
                <button type="button" className="secondary-btn" onClick={resetForm}>
                  Cancel
                </button>
              )}
            </div>
          </form>
        </section>

        <section className="panel">
          <h3>Course list</h3>

          {isLoading && <p>Loading courses...</p>}
          {isError && <p className="error-text">Error: {(error as Error).message}</p>}

          {data && (
            <table className="data-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Title</th>
                  <th>Description</th>
                  <th>Creator</th>
                  <th>Enrolled</th>
                  <th>Official</th>
                  <th>Year</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="empty-row">
                      No courses found.
                    </td>
                  </tr>
                ) : (
                  data.map((course) => (
                    <tr key={course.id}>
                      <td className="id-cell">{course.id}</td>
                      <td>{course.title}</td>
                      <td className="desc-cell">{course.description}</td>
                      <td>{course.creatorName}</td>
                      <td>{course.enrollmentCount}</td>
                      <td>{course.isOfficial ? 'Yes' : 'No'}</td>
                      <td>{course.schoolYear}</td>
                      <td>
                        <div className="table-actions">
                          <button type="button" className="secondary-btn" onClick={() => startEditing(course)}>
                            Edit
                          </button>
                          <button
                            type="button"
                            className="danger-btn"
                            onClick={() => handleDelete(course)}
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
