import { useQuery } from '@tanstack/react-query';
import { apiFetch } from '../lib/api';

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

// ---- API call ----

async function fetchCourses(): Promise<AdminCourse[]> {
  return apiFetch<AdminCourse[]>('/admin/courses');
}

// ---- Component ----

export default function Courses() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['admin-courses'],
    queryFn: fetchCourses,
  });

  return (
    <div className="page">
      <h2>Courses</h2>

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
            </tr>
          </thead>
          <tbody>
            {data.length === 0 ? (
              <tr>
                <td colSpan={7} className="empty-row">
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
                </tr>
              ))
            )}
          </tbody>
        </table>
      )}
    </div>
  );
}
