const state = {
  csrf: "",
  user: null,
  page: "dashboard",
  options: { departments: [], courses: [], students: [] },
  records: []
};

const pageInfo = {
  dashboard: ["OVERVIEW", "Dashboard", "A pulse check on your academic community."],
  students: ["ACADEMIC RECORDS", "Students", "Search, manage and export student records."],
  courses: ["ACADEMIC CATALOGUE", "Courses", "Manage programmes, fees and student enrolments."],
  departments: ["ACADEMIC CATALOGUE", "Departments", "Manage academic departments and their heads."],
  attendance: ["STUDENT ENGAGEMENT", "Attendance", "Mark daily attendance and review monthly summaries."],
  exams: ["ASSESSMENT", "Examinations", "Manage exams, enter marks and review results."],
  fees: ["FINANCE", "Fees", "Review fee balances, record payments and check receipts."],
  reports: ["INSIGHTS", "Reports", "Generate and export institution reports."]
};

const configs = {
  students: {
    endpoint: "/api/students",
    columns: [["studentCode", "Student ID"], ["fullName", "Full name"], ["email", "Email"], ["phone", "Phone"], ["deptName", "Department"], ["courseName", "Course"], ["semester", "Semester"]],
    fields: [
      ["studentCode", "Student ID (blank for automatic)", "text", false],
      ["fullName", "Full name", "text", true], ["dob", "Date of birth", "date", true],
      ["gender", "Gender", "select:gender", true], ["email", "Email", "email", true],
      ["phone", "Phone", "tel", true], ["address", "Address", "textarea", false],
      ["deptId", "Department", "select:departments", true], ["courseId", "Course", "select:courses", true],
      ["academicYear", "Academic year (e.g. 2026-2027)", "text", true],
      ["admissionDate", "Admission date", "date", true], ["semester", "Semester", "number", true]
    ],
    search: true,
    edit: true,
    remove: true
  },
  courses: {
    endpoint: "/api/courses",
    columns: [["courseCode", "Code"], ["courseName", "Course"], ["deptName", "Department"], ["durationYears", "Duration"], ["courseFee", "Fee"], ["studentCount", "Students"]],
    fields: [
      ["courseCode", "Course code", "text", true], ["courseName", "Course name", "text", true],
      ["deptId", "Department", "select:departments", true], ["durationYears", "Duration (years)", "number", true],
      ["courseFee", "Course fee", "number", true], ["description", "Description", "textarea", false]
    ],
    edit: true,
    remove: true,
    enroll: true,
    roster: true
  },
  departments: {
    endpoint: "/api/departments",
    columns: [["deptCode", "Code"], ["deptName", "Department"], ["hodName", "Head of department"], ["courseCount", "Courses"], ["studentCount", "Students"]],
    fields: [["deptCode", "Department code", "text", true], ["deptName", "Department name", "text", true], ["hodName", "Head of department", "text", true]],
    edit: true,
    remove: true
  },
  exams: {
    endpoint: "/api/exams",
    columns: [["examName", "Examination"], ["courseName", "Course"], ["examDate", "Date"], ["examType", "Type"], ["maxMarks", "Maximum marks"]],
    fields: [["examName", "Examination name", "text", true], ["courseId", "Course", "select:courses", true], ["examDate", "Date", "date", true], ["maxMarks", "Maximum marks", "number", true], ["examType", "Type", "select:examTypes", true]],
    edit: true,
    remove: true
  }
};

const $ = selector => document.querySelector(selector);
const pageContent = $("#pageContent");
const formDialog = $("#formDialog");
const recordForm = $("#recordForm");

function node(tag, className, text) {
  const item = document.createElement(tag);
  if (className) item.className = className;
  if (text !== undefined && text !== null) item.textContent = String(text);
  return item;
}

function button(text, className, onClick) {
  const item = node("button", `button ${className || "secondary"}`, text);
  item.type = "button";
  item.addEventListener("click", onClick);
  return item;
}

async function api(url, options = {}) {
  const headers = new Headers(options.headers || {});
  if (options.body && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (options.method && options.method !== "GET") {
    headers.set("X-CSRF-TOKEN", state.csrf);
  }
  const response = await fetch(url, { ...options, headers, credentials: "same-origin" });
  const contentType = response.headers.get("content-type") || "";
  const body = contentType.includes("application/json")
    ? await response.json()
    : await response.text();
  if (!response.ok) {
    if (response.status === 401 && !url.endsWith("/login")) {
      showLogin();
    }
    const message = body && typeof body === "object" ? body.message : "";
    throw new Error(message || `Request failed (${response.status}).`);
  }
  return body;
}

function showNotice(message, error = false) {
  const notice = $("#notice");
  notice.textContent = message;
  notice.classList.toggle("error", error);
  notice.classList.remove("hidden");
  window.clearTimeout(showNotice.timer);
  showNotice.timer = window.setTimeout(() => notice.classList.add("hidden"), 5000);
}

function showLogin(message = "") {
  state.user = null;
  $("#application").classList.add("hidden");
  $("#loginScreen").classList.remove("hidden");
  $("#loginMessage").textContent = message;
}

function showApplication(user) {
  state.user = user;
  $("#loginScreen").classList.add("hidden");
  $("#application").classList.remove("hidden");
  $("#userName").textContent = user.fullName;
  $("#userRole").textContent = `${user.role} · ${user.username}`;
  $("#userAvatar").textContent = (user.fullName || user.username || "A").slice(0, 1).toUpperCase();
}

async function initialise() {
  try {
    const session = await api("/api/session");
    state.csrf = session.csrfToken;
    if (session.authenticated) {
      showApplication(session.user);
      await refreshOptions();
      await loadPage(state.page);
    } else {
      showLogin();
    }
  } catch (error) {
    showLogin(error.message);
  }
}

async function refreshOptions() {
  state.options = await api("/api/options");
}

$("#loginForm").addEventListener("submit", async event => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const button = event.currentTarget.querySelector("button[type=submit]");
  button.disabled = true;
  $("#loginMessage").textContent = "";
  try {
    const result = await api("/api/login", {
      method: "POST",
      body: JSON.stringify({ username: form.get("username"), password: form.get("password") })
    });
    state.csrf = result.csrfToken;
    showApplication(result.user);
    await refreshOptions();
    await loadPage("dashboard");
  } catch (error) {
    $("#loginMessage").textContent = error.message;
  } finally {
    button.disabled = false;
  }
});

$("#logoutButton").addEventListener("click", async () => {
  try {
    await api("/api/logout", { method: "POST" });
    const session = await api("/api/session");
    state.csrf = session.csrfToken;
    showLogin("You have signed out.");
  } catch (error) {
    showNotice(error.message, true);
  }
});

$("#navigation").addEventListener("click", event => {
  const target = event.target.closest("[data-page]");
  if (target) loadPage(target.dataset.page);
});

document.querySelector(".brand").addEventListener("click", event => {
  event.preventDefault();
  loadPage("dashboard");
});

document.querySelectorAll("[data-close-dialog]").forEach(item =>
  item.addEventListener("click", () => item.closest("dialog").close()));

$("#passwordButton").addEventListener("click", () => {
  $("#passwordError").textContent = "";
  $("#passwordDialog").showModal();
});

$("#passwordForm").addEventListener("submit", async event => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  try {
    const result = await api("/api/change-password", {
      method: "POST",
      body: JSON.stringify({
        currentPassword: form.get("currentPassword"),
        newPassword: form.get("newPassword"),
        confirmPassword: form.get("confirmPassword")
      })
    });
    event.currentTarget.reset();
    $("#passwordDialog").close();
    showNotice(result.message);
  } catch (error) {
    $("#passwordError").textContent = error.message;
  }
});

function setHeader(page) {
  state.page = page;
  const [eyebrow, title, description] = pageInfo[page] || pageInfo.dashboard;
  $("#pageEyebrow").textContent = eyebrow;
  $("#pageTitle").textContent = title;
  $("#pageDescription").textContent = description;
  $("#breadcrumb").textContent = title;
  $("#pageActions").replaceChildren();
  document.querySelectorAll(".nav-item[data-page]").forEach(item =>
    item.classList.toggle("active", item.dataset.page === page));
}

async function loadPage(page) {
  setHeader(page);
  pageContent.replaceChildren(node("div", "spinner"));
  try {
    if (page === "dashboard") await renderDashboard();
    else if (page === "attendance") await renderAttendance();
    else if (page === "fees") await renderFees();
    else if (page === "reports") renderReports();
    else if (page === "exams") await renderExams();
    else await renderCrud(page);
  } catch (error) {
    pageContent.replaceChildren(emptyState("Could not load this page", error.message));
    showNotice(error.message, true);
  }
}

function emptyState(title, message) {
  const box = node("div", "empty-state");
  box.append(node("b", "", title), node("span", "", message || "Try changing the filters or refresh."));
  return box;
}

function panel(title, subtitle) {
  const box = node("section", "panel");
  const head = node("div", "panel-title");
  const text = node("div");
  text.append(node("h2", "", title));
  if (subtitle) text.append(node("p", "", subtitle));
  head.append(text);
  box.append(head);
  return box;
}

function metricCard(title, value, note, icon) {
  const card = node("article", "metric-card");
  const top = node("div", "metric-top");
  top.append(node("span", "", title), node("span", "metric-icon", icon));
  card.append(top, node("div", "metric-value", value ?? "0"), node("div", "metric-note", note || "Institution total"));
  return card;
}

async function renderDashboard() {
  const stats = await api("/api/dashboard");
  const cards = node("div", "cards");
  cards.append(
    metricCard("Students", stats.totalStudents, `${stats.maleStudents} male · ${stats.femaleStudents} female`, "♙"),
    metricCard("Courses", stats.totalCourses, "Programmes offered", "▤"),
    metricCard("Departments", stats.totalDepartments, `${stats.totalEnrollments} active enrolments`, "⌂"),
    metricCard("Average attendance", `${Number(stats.averageAttendance || 0).toFixed(1)}%`, "Across recorded sessions", "✓")
  );
  const grid = node("div", "dashboard-grid");
  const departmentsPanel = panel("Students by department", "Current enrolment distribution");
  departmentsPanel.append(renderBars(stats.departmentWise || []));
  const coursePanel = panel("Fee overview", "Collected and outstanding balances");
  coursePanel.append(renderFeeBreakdown(stats.feeCollected, stats.feePending));
  grid.append(departmentsPanel, coursePanel);
  const recent = panel("Recent registrations", "Latest student records");
  recent.append(renderTable(stats.recentStudents || [], [
    ["studentCode", "Student ID"], ["fullName", "Student"], ["courseName", "Course"],
    ["deptName", "Department"], ["admissionDate", "Admission"]
  ]));
  pageContent.replaceChildren(cards, grid, recent);
}

function renderBars(rows) {
  if (!rows.length) return emptyState("No department data", "Departments will appear here once they have students.");
  const max = Math.max(...rows.map(row => Number(row.value) || 0), 1);
  const list = node("div", "bar-list");
  rows.forEach(row => {
    const item = node("div", "bar-item");
    const label = node("span", "bar-label", row.label);
    label.title = row.label;
    const track = node("div", "bar-track");
    const fill = node("div", "bar-fill");
    fill.style.width = `${Math.max(3, (Number(row.value) || 0) * 100 / max)}%`;
    track.append(fill);
    item.append(label, track, node("span", "bar-count", Math.round(row.value || 0)));
    list.append(item);
  });
  return list;
}

function renderFeeBreakdown(collected, pending) {
  const total = (Number(collected) || 0) + (Number(pending) || 0);
  const box = node("div", "bar-list");
  const percent = total ? Number(collected) * 100 / total : 0;
  const value = node("div", "metric-value", formatMoney(collected));
  value.style.fontSize = "22px";
  const note = node("p", "detail-text", `${formatMoney(pending)} outstanding`);
  const track = node("div", "bar-track");
  track.style.height = "11px";
  const fill = node("div", "bar-fill");
  fill.style.width = `${percent}%`;
  track.append(fill);
  const status = node("div", "metric-note", `${percent.toFixed(1)}% of fees collected`);
  box.append(value, note, track, status);
  return box;
}

function formatMoney(value) {
  return new Intl.NumberFormat(undefined, { style: "currency", currency: "INR", maximumFractionDigits: 2 }).format(Number(value) || 0);
}

function renderTable(rows, columns, actions) {
  if (!rows.length) return emptyState("Nothing to show yet", "Records that match your request will appear here.");
  const wrap = node("div", "table-wrap");
  const table = node("table");
  const head = node("thead");
  const headRow = node("tr");
  columns.forEach(([, label]) => headRow.append(node("th", "", label)));
  if (actions && actions.length) headRow.append(node("th", "", "Actions"));
  head.append(headRow);
  const body = node("tbody");
  rows.forEach(row => {
    const tr = node("tr");
    columns.forEach(([key]) => {
      const td = node("td");
      const value = row[key];
      if (key === "paymentStatus" || key === "status" || key === "examType") {
        td.append(node("span", `tag ${String(value || "").toLowerCase()}`, value || "—"));
      } else if (key === "courseFee" || key === "totalFee" || key === "paidAmount" || key === "remainingAmount" || key === "amount") {
        td.textContent = formatMoney(value);
      } else if (key === "fullName" || key === "studentName") {
        td.append(node("span", "table-name", value || "—"));
      } else {
        td.textContent = value === null || value === undefined || value === "" ? "—" : String(value);
      }
      tr.append(td);
    });
    if (actions && actions.length) {
      const td = node("td");
      const buttons = node("div", "table-actions");
      actions.forEach(action => {
        const item = node("button", action.danger ? "delete" : "", action.label);
        item.type = "button";
        item.addEventListener("click", () => action.run(row));
        buttons.append(item);
      });
      td.append(buttons);
      tr.append(td);
    }
    body.append(tr);
  });
  table.append(head, body);
  wrap.append(table);
  return wrap;
}

async function renderCrud(page) {
  const config = configs[page];
  if (!config) return;
  const toolbar = node("div", "toolbar");
  if (config.search) {
    const search = node("input");
    search.type = "search";
    search.placeholder = "Search by name, ID or email";
    search.setAttribute("aria-label", "Search students");
    search.addEventListener("keydown", event => {
      if (event.key === "Enter") loadCrudRows(page, search.value);
    });
    toolbar.append(search, button("Search", "secondary", () => loadCrudRows(page, search.value)));
  }
  toolbar.append(button(`＋  Add ${page === "students" ? "student" : page === "courses" ? "course" : page === "departments" ? "department" : "examination"}`,
    "primary", () => openRecordForm(page)));
  const card = node("section", "data-card");
  card.append(node("div", "table-caption", "Live records · changes are saved directly to MySQL"));
  const tableArea = node("div", "table-wrap");
  card.append(tableArea);
  pageContent.replaceChildren(toolbar, card);
  await loadCrudRows(page, "", tableArea);
}

async function loadCrudRows(page, search = "", tableArea) {
  const config = configs[page];
  const area = tableArea || pageContent.querySelector(".data-card .table-wrap");
  if (!area) return;
  try {
    let rows;
    if (page === "students") {
      const query = new URLSearchParams();
      if (search) query.set("search", search);
      rows = await api(`${config.endpoint}?${query}`);
    } else {
      rows = await api(config.endpoint);
    }
    state.records = rows;
    const actions = [];
    if (config.edit) actions.push({ label: "Edit", run: row => openRecordForm(page, row) });
    if (config.enroll) actions.push({ label: "Enroll", run: row => openEnrollmentForm(row) });
    if (config.roster) actions.push({ label: "Roster", run: row => showRoster(row) });
    if (config.remove) actions.push({ label: "Delete", danger: true, run: row => removeRecord(page, row) });
    area.replaceChildren(renderTable(rows, config.columns, actions));
  } catch (error) {
    area.replaceChildren(emptyState("Could not load records", error.message));
  }
}

function openRecordForm(page, row = null, override = null) {
  const config = override || configs[page];
  $("#formTitle").textContent = row ? "Edit record" : "Add record";
  $("#formError").textContent = "";
  $("#formFields").replaceChildren();
  config.fields.forEach(([key, title, type, required]) => {
    const label = node("label", type === "textarea" ? "full" : "");
    label.append(node("span", "", title));
    let control;
    if (type.startsWith("select:")) {
      control = node("select");
      const kind = type.slice(7);
      const items = kind === "departments" ? state.options.departments
        : kind === "courses" ? state.options.courses
        : kind === "students" ? state.options.students
        : kind === "fees" ? state.options.feeList || [] : [];
      const empty = node("option", "", kind === "gender" ? "Choose gender" : "Select an option");
      empty.value = "";
      control.append(empty);
      if (kind === "gender") {
        ["Male", "Female", "Other"].forEach(value => {
          const option = node("option", "", value);
          option.value = value;
          control.append(option);
        });
      } else if (kind === "examTypes") {
        ["INTERNAL", "MIDTERM", "SEMESTER", "PRACTICAL"].forEach(value => {
          const option = node("option", "", value);
          option.value = value;
          control.append(option);
        });
      } else {
        items.forEach(item => {
          const name = kind === "departments" ? item.deptName
            : kind === "courses" ? item.courseName
            : kind === "fees" ? `${item.studentCode} · ${item.studentName} · ${formatMoney(item.remainingAmount)} due`
            : `${item.studentCode} · ${item.fullName}`;
          const id = kind === "departments" ? item.deptId
            : kind === "courses" ? item.courseId
            : kind === "fees" ? item.feeId : item.studentId;
          const option = node("option", "", name);
          option.value = id;
          control.append(option);
        });
      }
    } else if (type === "textarea") {
      control = node("textarea");
    } else {
      control = node("input");
      control.type = type;
      if (type === "number") control.step = key === "semester" || key === "durationYears" ? "1" : "0.01";
    }
    control.name = key;
    control.required = required;
    if (key === "semester") {
      control.min = "1";
      control.max = "8";
      control.value = "1";
    }
    if (key === "durationYears") control.value = "4";
    if (key === "courseFee" || key === "maxMarks") control.min = "0";
    if (key === "dob") control.max = new Date().toISOString().slice(0, 10);
    if (key === "admissionDate" || key === "examDate") control.value = new Date().toISOString().slice(0, 10);
    if (row && row[key] !== null && row[key] !== undefined) control.value = row[key];
    label.append(control);
    $("#formFields").append(label);
  });
  recordForm.onsubmit = async event => {
    event.preventDefault();
    const body = {};
    for (const [key, , type] of config.fields) {
      const value = recordForm.elements.namedItem(key).value;
      body[key] = type === "number" ? Number(value) : value;
    }
    const endpoint = config.endpoint || "/api/courses";
    const method = row ? "PUT" : "POST";
    const url = row && config.idPath ? `${endpoint}/${row[config.idPath]}`
      : row ? `${endpoint}/${recordId(page, row)}` : endpoint;
    try {
      const result = await api(url, { method, body: JSON.stringify(body) });
      formDialog.close();
      showNotice(result.message || "Record saved.");
      await refreshOptions();
      await loadPage(page);
    } catch (error) {
      $("#formError").textContent = error.message;
    }
  };
  formDialog.showModal();
}

function recordId(page, row) {
  return page === "students" ? row.studentId
    : page === "courses" ? row.courseId
    : page === "departments" ? row.deptId
    : row.examId;
}

async function removeRecord(page, row) {
  const labels = { students: row.fullName, courses: row.courseName, departments: row.deptName, exams: row.examName };
  if (!window.confirm(`Delete "${labels[page]}"? This action cannot be undone.`)) return;
  const id = recordId(page, row);
  try {
    const result = await api(`${configs[page].endpoint}/${id}`, { method: "DELETE" });
    showNotice(result.message || "Record deleted.");
    await refreshOptions();
    await loadPage(page);
  } catch (error) {
    showNotice(error.message, true);
  }
}

function openEnrollmentForm(course) {
  openRecordForm("courses", null, {
    endpoint: `/api/courses/${course.courseId}/enrollments`,
    fields: [["studentId", "Student", "select:students", true]],
    idPath: "enrollmentId"
  });
  $("#formTitle").textContent = `Enroll in ${course.courseName}`;
  recordForm.onsubmit = async event => {
    event.preventDefault();
    const studentId = Number(recordForm.elements.namedItem("studentId").value);
    try {
      const result = await api(`/api/courses/${course.courseId}/enrollments`, {
        method: "POST", body: JSON.stringify({ studentId })
      });
      formDialog.close();
      showNotice(result.message);
      await refreshOptions();
      await loadPage("courses");
    } catch (error) {
      $("#formError").textContent = error.message;
    }
  };
}

async function showRoster(course) {
  try {
    const rows = await api(`/api/courses/${course.courseId}/enrollments`);
    const section = panel(`${course.courseName} · enrolled students`, `${rows.length} enrolment${rows.length === 1 ? "" : "s"}`);
    section.append(renderTable(rows, [
      ["studentCode", "Student ID"], ["studentName", "Student"], ["enrollDate", "Enrolled"], ["status", "Status"]
    ], [{ label: "Remove", danger: true, run: async row => {
      if (!window.confirm(`Remove ${row.studentName} from this course?`)) return;
      try {
        await api(`/api/enrollments/${row.enrollmentId}`, { method: "DELETE" });
        showNotice("Enrolment removed.");
        await showRoster(course);
      } catch (error) { showNotice(error.message, true); }
    } }]));
    const existing = pageContent.querySelector(".roster-panel");
    if (existing) existing.remove();
    section.classList.add("roster-panel");
    pageContent.append(section);
    section.scrollIntoView({ behavior: "smooth", block: "nearest" });
  } catch (error) {
    showNotice(error.message, true);
  }
}

async function renderAttendance() {
  const options = state.options.courses;
  const filters = panel("Daily attendance", "Mark all enrolled students for a course and date.");
  const form = node("form", "inline-form");
  form.append(selectControl("Course", "courseId", options, "courseId", "courseName", true),
    inputControl("Date", "date", "date", true, new Date().toISOString().slice(0, 10)),
    selectPlain("Status", "status", ["PRESENT", "ABSENT", "LATE"], "PRESENT"),
    button("Mark all", "primary", () => {}));
  const markButton = form.querySelector("button");
  markButton.type = "submit";
  form.addEventListener("submit", async event => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(form));
    try {
      const result = await api("/api/attendance/bulk", {
        method: "POST",
        body: JSON.stringify({ courseId: Number(data.courseId), date: data.date, status: data.status })
      });
      showNotice(`${result.message} ${result.updated} student records updated.`);
      await loadDailyAttendance(form, dailyArea);
    } catch (error) { showNotice(error.message, true); }
  });
  const loadButton = button("Load day", "secondary", () => loadDailyAttendance(form, dailyArea));
  form.append(loadButton);
  filters.append(form);
  const daily = panel("Attendance sheet", "Change an individual student's status for the selected date.");
  const dailyArea = node("div");
  daily.append(dailyArea);
  const monthPanel = panel("Monthly summary", "Review percentages and shortage indicators.");
  const monthForm = node("form", "inline-form");
  monthForm.append(inputControl("Month", "month", "month", true, new Date().toISOString().slice(0, 7)),
    selectControl("Course", "courseId", options, "courseId", "courseName", false),
    button("View summary", "secondary", () => {}));
  monthForm.querySelector("button").type = "submit";
  const monthArea = node("div");
  monthForm.addEventListener("submit", async event => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(monthForm));
    const query = new URLSearchParams({ month: data.month, courseId: data.courseId || "0" });
    try {
      const rows = await api(`/api/attendance/monthly?${query}`);
      monthArea.replaceChildren(renderTable(rows, [
        ["studentCode", "Student ID"], ["studentName", "Student"], ["courseName", "Course"],
        ["presentCount", "Present"], ["absentCount", "Absent"], ["lateCount", "Late"],
        ["percentage", "Attendance %"], ["statusLabel", "Remark"]
      ]));
    } catch (error) { monthArea.replaceChildren(emptyState("Could not load report", error.message)); }
  });
  monthPanel.append(monthForm, monthArea);
  pageContent.replaceChildren(filters, daily, monthPanel);
}

async function loadDailyAttendance(form, area) {
  const data = Object.fromEntries(new FormData(form));
  if (!data.date) return;
  const query = new URLSearchParams({ date: data.date, courseId: data.courseId || "0" });
  try {
    const rows = await api(`/api/attendance?${query}`);
    const table = renderTable(rows, [
      ["studentCode", "Student ID"], ["studentName", "Student"], ["courseName", "Course"], ["status", "Status"]
    ]);
    if (!rows.length) {
      area.replaceChildren(table);
      return;
    }
    area.replaceChildren(table);
    const bodyRows = area.querySelectorAll("tbody tr");
    bodyRows.forEach((tr, index) => {
      const row = rows[index];
      const cell = node("td");
      const controls = node("div", "table-actions");
      const select = node("select", "status-select");
      ["PRESENT", "ABSENT", "LATE"].forEach(value => {
        const option = node("option", "", value);
        option.value = value;
        option.selected = value === row.status;
        select.append(option);
      });
      controls.append(select, button("Save", "secondary", async () => {
        try {
          const result = await api("/api/attendance", {
            method: "POST",
            body: JSON.stringify({ studentId: row.studentId, courseId: row.courseId, date: data.date, status: select.value })
          });
          showNotice(result.message);
        } catch (error) { showNotice(error.message, true); }
      }));
      cell.append(controls);
      tr.append(cell);
    });
    const th = area.querySelector("thead tr");
    th.append(node("th", "", "Update"));
  } catch (error) {
    area.replaceChildren(emptyState("Could not load attendance", error.message));
  }
}

function inputControl(labelText, name, type, required, value = "") {
  const label = node("label");
  label.append(node("span", "", labelText));
  const input = node("input");
  input.name = name;
  input.type = type;
  input.required = required;
  input.value = value;
  label.append(input);
  return label;
}

function selectControl(labelText, name, items, valueKey, labelKey, required) {
  const label = node("label");
  label.append(node("span", "", labelText));
  const select = node("select");
  select.name = name;
  select.required = required;
  const empty = node("option", "", "Select");
  empty.value = "";
  select.append(empty);
  items.forEach(item => {
    const option = node("option", "", item[labelKey]);
    option.value = item[valueKey];
    select.append(option);
  });
  label.append(select);
  return label;
}

function selectPlain(labelText, name, values, selected) {
  const label = node("label");
  label.append(node("span", "", labelText));
  const select = node("select");
  select.name = name;
  values.forEach(value => {
    const option = node("option", "", value);
    option.value = value;
    option.selected = value === selected;
    select.append(option);
  });
  label.append(select);
  return label;
}

async function renderExams() {
  const toolbar = node("div", "toolbar");
  toolbar.append(button("＋  Add examination", "primary", () => openRecordForm("exams")));
  const dataCard = node("section", "data-card");
  dataCard.append(node("div", "table-caption", "Examinations · create, edit and manage marks"));
  const examArea = node("div", "table-wrap");
  dataCard.append(examArea);
  const marks = panel("Enter marks", "Select an exam and student. Marks are checked against the maximum.");
  const form = node("form", "inline-form");
  const examSelect = selectControl("Examination", "examId", [], "examId", "examName", true);
  const studentSelect = selectControl("Student", "studentId", state.options.students, "studentId", "fullName", true);
  const markInput = inputControl("Marks obtained", "marks", "number", true, "");
  const submit = button("Save mark", "primary", () => {});
  submit.type = "submit";
  form.append(examSelect, studentSelect, markInput, submit);
  const examField = examSelect.querySelector("select");
  const maxLabel = node("span", "detail-text");
  examSelect.append(maxLabel);
  const resultsPanel = panel("Result sheet", "Course-wise totals, percentages and grades.");
  const resultForm = node("form", "inline-form");
  resultForm.append(selectControl("Course", "courseId", state.options.courses, "courseId", "courseName", false),
    button("Load results", "secondary", () => {}));
  resultForm.querySelector("button").type = "submit";
  const resultsArea = node("div");
  resultForm.addEventListener("submit", async event => {
    event.preventDefault();
    const courseId = new FormData(resultForm).get("courseId") || "0";
    try {
      const rows = await api(`/api/results?courseId=${encodeURIComponent(courseId)}`);
      resultsArea.replaceChildren(renderTable(rows, [
        ["studentCode", "Student ID"], ["studentName", "Student"], ["examCount", "Exams"],
        ["totalObtained", "Obtained"], ["totalMax", "Maximum"], ["percentage", "Percent"],
        ["grade", "Grade"], ["resultStatus", "Result"]
      ]));
    } catch (error) { resultsArea.replaceChildren(emptyState("Could not load results", error.message)); }
  });
  resultsPanel.append(resultForm, resultsArea);
  const markPanel = node("section", "panel");
  const markHead = node("div", "panel-title");
  markHead.append(node("div"));
  markHead.firstChild.append(node("h2", "", "Enter marks"), node("p", "", "Select an exam and student. Marks are checked against the maximum."));
  markPanel.append(markHead, form, maxLabel);
  examField.addEventListener("change", async () => {
    const selected = state.options.examList.find(item => String(item.examId) === examField.value);
    if (selected) markInput.querySelector("input").max = selected.maxMarks;
    await loadMarksTable(examField.value, markPanel);
  });
  form.addEventListener("submit", async event => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(form));
    try {
      const result = await api("/api/marks", { method: "POST", body: JSON.stringify({
        examId: Number(data.examId), studentId: Number(data.studentId), marks: Number(data.marks)
      }) });
      showNotice(result.message);
      await loadMarksTable(data.examId, markPanel);
    } catch (error) { showNotice(error.message, true); }
  });
  pageContent.replaceChildren(toolbar, dataCard, markPanel, resultsPanel);
  try {
    const list = await api("/api/exams");
    state.options.examList = list;
    examField.replaceChildren(node("option", "", "Select examination"));
    examField.options[0].value = "";
    list.forEach(item => {
      const option = node("option", "", `${item.examName} · ${item.courseName}`);
      option.value = item.examId;
      examField.append(option);
    });
    examArea.replaceChildren(renderTable(list, configs.exams.columns, [
      { label: "Edit", run: row => openRecordForm("exams", row) },
      { label: "Marks", run: async row => {
        examField.value = row.examId;
        await loadMarksTable(row.examId, markPanel);
        markPanel.scrollIntoView({ behavior: "smooth", block: "center" });
      } },
      { label: "Delete", danger: true, run: row => removeRecord("exams", row) }
    ]));
  } catch (error) {
    examArea.replaceChildren(emptyState("Could not load examinations", error.message));
  }
}

async function loadMarksTable(examId, container) {
  if (!examId) return;
  try {
    const rows = await api(`/api/exams/${examId}/marks`);
    const prior = container.querySelector(".marks-table");
    if (prior) prior.remove();
    const area = node("div", "marks-table");
    area.append(renderTable(rows, [["studentCode", "Student ID"], ["studentName", "Student"], ["marksObtained", "Marks"]]));
    container.append(area);
  } catch (error) { showNotice(error.message, true); }
}

async function renderFees() {
  const toolbar = node("div", "toolbar");
  const search = node("input");
  search.type = "search";
  search.placeholder = "Search student";
  const status = node("select");
  [["", "All statuses"], ["PENDING", "Pending"], ["PARTIALLY_PAID", "Partially paid"], ["PAID", "Paid"]].forEach(([value, label]) => {
    const option = node("option", "", label);
    option.value = value;
    status.append(option);
  });
  const refresh = button("Apply filters", "secondary", () => loadFees(search.value, status.value, tableArea));
  toolbar.append(search, status, refresh, button("Payment history", "secondary", () => loadPaymentHistory()));
  const card = node("section", "data-card");
  card.append(node("div", "table-caption", "Fee accounts · balances update automatically after payments"));
  const tableArea = node("div", "table-wrap");
  card.append(tableArea);
  pageContent.replaceChildren(toolbar, card);
  await loadFees("", "", tableArea);
}

async function loadFees(search, status, area) {
  try {
    const query = new URLSearchParams({ search, status });
    const rows = await api(`/api/fees?${query}`);
    area.replaceChildren(renderTable(rows, [
      ["studentCode", "Student ID"], ["studentName", "Student"], ["courseName", "Course"],
      ["totalFee", "Total"], ["paidAmount", "Paid"], ["remainingAmount", "Balance"], ["paymentStatus", "Status"]
    ], [{ label: "Record payment", run: row => openPaymentForm(row) }]));
  } catch (error) { area.replaceChildren(emptyState("Could not load fee accounts", error.message)); }
}

function openPaymentForm(fee) {
  const override = {
    endpoint: "/api/payments",
    fields: [
      ["feeId", "Fee account", "select:fees", true],
      ["amount", `Payment amount (balance ${formatMoney(fee.remainingAmount)})`, "number", true],
      ["date", "Payment date", "date", true],
      ["mode", "Payment method", "select:paymentModes", true],
      ["remarks", "Remarks", "text", false]
    ]
  };
  state.options.feeList = [fee];
  openRecordForm("fees", null, override);
  $("#formTitle").textContent = `Payment · ${fee.studentName}`;
  const feeSelect = recordForm.elements.namedItem("feeId");
  feeSelect.value = fee.feeId;
  feeSelect.disabled = true;
  feeSelect.required = false;
  const modeSelect = recordForm.elements.namedItem("mode");
  modeSelect.replaceChildren(...["CASH", "CARD", "UPI", "NET_BANKING", "CHEQUE"].map(value => {
    const option = node("option", "", value);
    option.value = value;
    return option;
  }));
  recordForm.elements.namedItem("date").value = new Date().toISOString().slice(0, 10);
  recordForm.elements.namedItem("amount").max = fee.remainingAmount;
  recordForm.onsubmit = async event => {
    event.preventDefault();
    const data = Object.fromEntries(new FormData(recordForm));
    try {
      const result = await api("/api/payments", { method: "POST", body: JSON.stringify({
        feeId: fee.feeId, amount: Number(data.amount), date: data.date, mode: data.mode, remarks: data.remarks
      }) });
      formDialog.close();
      showNotice(result.message);
      await loadPage("fees");
    } catch (error) { $("#formError").textContent = error.message; }
  };
}

async function loadPaymentHistory() {
  try {
    const rows = await api("/api/payments");
    const card = node("section", "data-card");
    card.append(node("div", "table-caption", "Payment history · recorded transactions"));
    card.append(renderTable(rows, [
      ["paymentId", "Receipt"], ["studentCode", "Student ID"], ["studentName", "Student"],
      ["amount", "Amount"], ["paymentDate", "Date"], ["mode", "Method"], ["remarks", "Remarks"]
    ], [{ label: "Delete", danger: true, run: async row => {
      if (!window.confirm(`Delete receipt #${row.paymentId}?`)) return;
      try {
        const result = await api(`/api/payments/${row.paymentId}`, { method: "DELETE" });
        showNotice(result.message);
        await loadPaymentHistory();
      } catch (error) { showNotice(error.message, true); }
    } }]));
    pageContent.replaceChildren(button("←  Fee accounts", "secondary", () => loadPage("fees")), card);
  } catch (error) { showNotice(error.message, true); }
}

function renderReports() {
  const controls = node("form", "report-controls");
  const typeLabel = node("label");
  typeLabel.append(node("span", "", "Report"));
  const type = node("select");
  type.name = "type";
  [
    ["summary", "Institute summary"], ["department", "Department students"], ["course", "Course students"],
    ["attendance", "Monthly attendance"], ["examinations", "Examination results"],
    ["fees", "Fee status"], ["payments", "Payment history"]
  ].forEach(([value, label]) => {
    const option = node("option", "", label);
    option.value = value;
    type.append(option);
  });
  typeLabel.append(type);
  controls.append(typeLabel);
  const courseLabel = node("label");
  courseLabel.append(node("span", "", "Course"));
  const course = node("select");
  course.name = "courseId";
  course.append(option("", "All courses"));
  state.options.courses.forEach(item => course.append(option(item.courseId, item.courseName)));
  courseLabel.append(course);
  const deptLabel = node("label");
  deptLabel.append(node("span", "", "Department"));
  const dept = node("select");
  dept.name = "deptId";
  dept.append(option("", "Select department"));
  state.options.departments.forEach(item => dept.append(option(item.deptId, item.deptName)));
  deptLabel.append(dept);
  const month = inputControl("Month", "month", "month", false, new Date().toISOString().slice(0, 7));
  const statusLabel = node("label");
  statusLabel.append(node("span", "", "Fee status"));
  const feeStatus = node("select");
  feeStatus.name = "status";
  [["", "All"], ["PENDING", "Pending"], ["PARTIALLY_PAID", "Partially paid"], ["PAID", "Paid"]]
    .forEach(([value, label]) => feeStatus.append(option(value, label)));
  statusLabel.append(feeStatus);
  const run = button("Generate", "primary", () => {});
  run.type = "submit";
  controls.append(courseLabel, deptLabel, month, statusLabel, run);
  const exports = node("div", "toolbar");
  const csvButton = button("Export CSV", "secondary", () => downloadReport(controls, "csv"));
  const pdfButton = button("Export PDF", "secondary", () => downloadReport(controls, "pdf"));
  exports.append(csvButton, pdfButton);
  const reportCard = node("section", "data-card");
  reportCard.append(node("div", "table-caption", "Reports may be exported as CSV or PDF."));
  const area = node("div", "table-wrap");
  reportCard.append(area);
  controls.addEventListener("submit", async event => {
    event.preventDefault();
    await loadReport(controls, area);
  });
  type.addEventListener("change", () => updateReportControls(type.value, courseLabel, deptLabel, month, statusLabel));
  updateReportControls(type.value, courseLabel, deptLabel, month, statusLabel);
  pageContent.replaceChildren(controls, exports, reportCard);
}

function option(value, text) {
  const item = node("option", "", text);
  item.value = value;
  return item;
}

function updateReportControls(type, course, dept, month, status) {
  course.classList.toggle("hidden", !["course", "attendance", "examinations"].includes(type));
  dept.classList.toggle("hidden", type !== "department");
  month.classList.toggle("hidden", type !== "attendance");
  status.classList.toggle("hidden", type !== "fees");
}

function reportQuery(form) {
  const data = Object.fromEntries(new FormData(form));
  const query = new URLSearchParams({ type: data.type });
  ["courseId", "deptId", "month", "status"].forEach(key => {
    if (data[key]) query.set(key, data[key]);
  });
  return query;
}

async function loadReport(form, area) {
  try {
    const rows = await api(`/api/reports?${reportQuery(form)}`);
    area.replaceChildren(renderTable(rows.rows || [], (rows.headers || []).map((value, index) => [`col${index}`, value])));
    const tbody = area.querySelector("tbody");
    if (tbody) {
      rows.rows.forEach((row, rowIndex) => row.forEach((value, colIndex) => {
        const cell = tbody.rows[rowIndex]?.cells[colIndex];
        if (cell) cell.textContent = value ?? "—";
      }));
    }
  } catch (error) { area.replaceChildren(emptyState("Could not generate report", error.message)); }
}

function downloadReport(form, format) {
  const query = reportQuery(form);
  query.set("format", format);
  window.location.assign(`/api/reports/export?${query}`);
}

initialise();
