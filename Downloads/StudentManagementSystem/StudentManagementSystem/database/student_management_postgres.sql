DROP DATABASE IF EXISTS student_management;
CREATE DATABASE student_management;
\c student_management

CREATE TABLE admins (
    admin_id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ADMIN' CHECK (role IN ('ADMIN','STAFF')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE departments (
    dept_id SERIAL PRIMARY KEY,
    dept_code VARCHAR(10) NOT NULL UNIQUE,
    dept_name VARCHAR(100) NOT NULL UNIQUE,
    hod_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE courses (
    course_id SERIAL PRIMARY KEY,
    course_code VARCHAR(15) NOT NULL UNIQUE,
    course_name VARCHAR(100) NOT NULL,
    dept_id INT NOT NULL,
    duration_years INT NOT NULL DEFAULT 4 CHECK (duration_years BETWEEN 1 AND 6),
    course_fee NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (course_fee >= 0),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_course_dept FOREIGN KEY (dept_id)
        REFERENCES departments (dept_id) ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE TABLE students (
    student_id SERIAL PRIMARY KEY,
    student_code VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    dob DATE NOT NULL,
    gender VARCHAR(20) NOT NULL CHECK (gender IN ('Male','Female','Other')),
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(15) NOT NULL,
    address VARCHAR(255),
    dept_id INT NOT NULL,
    course_id INT NOT NULL,
    academic_year VARCHAR(15) NOT NULL,
    admission_date DATE NOT NULL,
    semester INT NOT NULL DEFAULT 1 CHECK (semester BETWEEN 1 AND 8),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_dept FOREIGN KEY (dept_id)
        REFERENCES departments (dept_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_student_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id) ON UPDATE CASCADE ON DELETE RESTRICT
);

CREATE INDEX idx_student_name ON students (full_name);
CREATE INDEX idx_student_dept ON students (dept_id);
CREATE INDEX idx_student_course ON students (course_id);

CREATE TABLE enrollments (
    enrollment_id SERIAL PRIMARY KEY,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    enroll_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','COMPLETED','DROPPED')),
    CONSTRAINT fk_enr_student FOREIGN KEY (student_id)
        REFERENCES students (student_id) ON DELETE CASCADE,
    CONSTRAINT fk_enr_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id) ON DELETE CASCADE,
    CONSTRAINT uq_enrollment UNIQUE (student_id, course_id)
);

CREATE TABLE attendance (
    attendance_id SERIAL PRIMARY KEY,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    att_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PRESENT','ABSENT','LATE')),
    marked_by INT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_att_student FOREIGN KEY (student_id)
        REFERENCES students (student_id) ON DELETE CASCADE,
    CONSTRAINT fk_att_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id) ON DELETE CASCADE,
    CONSTRAINT fk_att_admin FOREIGN KEY (marked_by)
        REFERENCES admins (admin_id) ON DELETE SET NULL,
    CONSTRAINT uq_attendance UNIQUE (student_id, course_id, att_date)
);

CREATE INDEX idx_att_date ON attendance (att_date);

CREATE TABLE examinations (
    exam_id SERIAL PRIMARY KEY,
    exam_name VARCHAR(100) NOT NULL,
    course_id INT NOT NULL,
    exam_date DATE NOT NULL,
    max_marks NUMERIC(6,2) NOT NULL CHECK (max_marks > 0),
    exam_type VARCHAR(20) NOT NULL CHECK (exam_type IN ('INTERNAL','MIDTERM','SEMESTER','PRACTICAL')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exam_course FOREIGN KEY (course_id)
        REFERENCES courses (course_id) ON DELETE CASCADE,
    CONSTRAINT uq_exam UNIQUE (exam_name, course_id)
);

CREATE TABLE marks (
    mark_id SERIAL PRIMARY KEY,
    exam_id INT NOT NULL,
    student_id INT NOT NULL,
    marks_obtained NUMERIC(6,2) NOT NULL CHECK (marks_obtained >= 0),
    CONSTRAINT fk_mark_exam FOREIGN KEY (exam_id)
        REFERENCES examinations (exam_id) ON DELETE CASCADE,
    CONSTRAINT fk_mark_student FOREIGN KEY (student_id)
        REFERENCES students (student_id) ON DELETE CASCADE,
    CONSTRAINT uq_mark UNIQUE (exam_id, student_id)
);

CREATE TABLE fees (
    fee_id SERIAL PRIMARY KEY,
    student_id INT NOT NULL UNIQUE,
    total_fee NUMERIC(10,2) NOT NULL CHECK (total_fee >= 0),
    paid_amount NUMERIC(10,2) NOT NULL DEFAULT 0.00 CHECK (paid_amount >= 0),
    remaining_amount NUMERIC(10,2) GENERATED ALWAYS AS (total_fee - paid_amount) STORED,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (payment_status IN ('PAID','PENDING','PARTIALLY_PAID')),
    CONSTRAINT fk_fee_student FOREIGN KEY (student_id)
        REFERENCES students (student_id) ON DELETE CASCADE
);

CREATE TABLE fee_payments (
    payment_id SERIAL PRIMARY KEY,
    fee_id INT NOT NULL,
    amount NUMERIC(10,2) NOT NULL CHECK (amount > 0),
    payment_date DATE NOT NULL,
    mode VARCHAR(20) NOT NULL CHECK (mode IN ('CASH','CARD','UPI','NET_BANKING','CHEQUE')),
    remarks VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_fee FOREIGN KEY (fee_id)
        REFERENCES fees (fee_id) ON DELETE CASCADE
);

CREATE OR REPLACE FUNCTION refresh_fee_summary() RETURNS TRIGGER AS $$
BEGIN
    UPDATE fees
       SET paid_amount = COALESCE((SELECT SUM(amount) FROM fee_payments WHERE fee_id = COALESCE(NEW.fee_id, OLD.fee_id)), 0),
           payment_status = CASE
               WHEN COALESCE((SELECT SUM(amount) FROM fee_payments WHERE fee_id = COALESCE(NEW.fee_id, OLD.fee_id)), 0) <= 0 THEN 'PENDING'
               WHEN COALESCE((SELECT SUM(amount) FROM fee_payments WHERE fee_id = COALESCE(NEW.fee_id, OLD.fee_id)), 0) >= fees.total_fee THEN 'PAID'
               ELSE 'PARTIALLY_PAID'
           END
     WHERE fee_id = COALESCE(NEW.fee_id, OLD.fee_id);
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_fee_payment_ai AFTER INSERT ON fee_payments
FOR EACH ROW EXECUTE FUNCTION refresh_fee_summary();

CREATE TRIGGER trg_fee_payment_au AFTER UPDATE ON fee_payments
FOR EACH ROW EXECUTE FUNCTION refresh_fee_summary();

CREATE TRIGGER trg_fee_payment_ad AFTER DELETE ON fee_payments
FOR EACH ROW EXECUTE FUNCTION refresh_fee_summary();

CREATE VIEW v_student_details AS
SELECT s.student_id, s.student_code, s.full_name, s.gender, s.email, s.phone,
       s.dob, s.address, s.academic_year, s.admission_date, s.semester,
       d.dept_code, d.dept_name, c.course_code, c.course_name
  FROM students s
  JOIN departments d ON d.dept_id = s.dept_id
  JOIN courses c ON c.course_id = s.course_id;

CREATE VIEW v_attendance_summary AS
SELECT a.student_id,
       SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS present_count,
       SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absent_count,
       SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END) AS late_count,
       COUNT(*) AS total_count
  FROM attendance a
 GROUP BY a.student_id;

CREATE VIEW v_student_results AS
SELECT m.student_id, e.exam_id, e.exam_name, e.max_marks, m.marks_obtained,
       ROUND((m.marks_obtained * 100.0) / e.max_marks, 2) AS percentage
  FROM marks m
  JOIN examinations e ON e.exam_id = m.exam_id;

CREATE VIEW v_fee_status AS
SELECT f.fee_id, s.student_code, s.full_name, f.total_fee, f.paid_amount,
       f.remaining_amount, f.payment_status
  FROM fees f
  JOIN students s ON s.student_id = f.student_id;

INSERT INTO admins (username, password_hash, full_name, role) VALUES
('admin', '$2a$12$0NIllu3MxLrnUeRDL18apeVuhfcvdVnWd1sFu8VBaOmw7gYDLC25m', 'System Administrator', 'ADMIN'),
('staff', '$2a$12$/pXqTUc5mr9d8pF8RGqzJOGyTHOfgqZ8PmFYiwcqabG8qULuTY5kq', 'Registrar Staff', 'STAFF');

INSERT INTO departments (dept_code, dept_name, hod_name) VALUES
('CSE','Computer Science & Engineering','Dr. R. K. Sharma'),
('IT','Information Technology','Dr. Anita Verma'),
('ECE','Electronics & Communication','Dr. S. Balaji'),
('ME','Mechanical Engineering','Dr. P. Nair'),
('CE','Civil Engineering','Dr. M. Gupta'),
('MBA','Business Administration','Dr. K. Iyer');

INSERT INTO courses (course_code, course_name, dept_id, duration_years, course_fee, description) VALUES
('CSE101','Computer Fundamentals',1,4,12000.00,'Core computing concepts'),
('CSE201','Data Structures',1,4,15000.00,'Algorithmic problem solving'),
('IT101','Web Technologies',2,4,13000.00,'HTML, CSS, JS and backend basics'),
('ECE101','Digital Electronics',3,4,14000.00,'Logic circuits and digital design'),
('ME101','Engineering Mechanics',4,4,12500.00,'Mechanical systems and forces'),
('MBA101','Business Management',6,2,11000.00,'Foundation business studies');

GRANT ALL PRIVILEGES ON DATABASE student_management TO sms_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO sms_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO sms_user;
