const state = {
  reviews: [],
  selected: null,
  action: null,
};

const els = {
  queue: document.querySelector('#queue-view'),
  detail: document.querySelector('#detail-view'),
  about: document.querySelector('#about-view'),
  list: document.querySelector('#review-list'),
  summary: document.querySelector('#summary-grid'),
  feedback: document.querySelector('#feedback'),
  detailBody: document.querySelector('#review-detail'),
  search: document.querySelector('#search-input'),
  filter: document.querySelector('#status-filter'),
  refresh: document.querySelector('#refresh-button'),
  back: document.querySelector('#back-button'),
  dialog: document.querySelector('#action-dialog'),
  actionForm: document.querySelector('#action-form'),
  dialogTitle: document.querySelector('#dialog-title'),
  dialogEyebrow: document.querySelector('#dialog-eyebrow'),
  dialogDescription: document.querySelector('#dialog-description'),
  dialogComment: document.querySelector('#dialog-comment'),
  commentField: document.querySelector('#comment-field'),
  dialogSubmit: document.querySelector('#dialog-submit'),
  dialogCancel: document.querySelector('#dialog-cancel'),
};

const statusConfig = {
  PENDING_TO_REVIEW: { label: 'TO review', className: 'pending-to' },
  PENDING_MISO_REVIEW: { label: 'MISO review', className: 'pending-miso' },
  REWORK_REQUIRED: { label: 'Rework required', className: 'rework' },
  COMPLETED: { label: 'Completed', className: 'completed' },
};

const actionConfig = {
  accept: {
    eyebrow: 'Transmission Owner decision',
    title: 'Accept DPP result?',
    description: 'This completes the review without a correction.',
    submitLabel: 'Accept result',
    endpoint: id => `/api/dpp-reviews/${id}/accept`,
    requiresComment: false,
    method: 'POST',
  },
  correction: {
    eyebrow: 'Transmission Owner correction',
    title: 'Submit correction',
    description: 'Describe what should be corrected so MISO can review the request.',
    submitLabel: 'Submit correction',
    endpoint: id => `/api/dpp-reviews/${id}/corrections`,
    requiresComment: true,
    method: 'POST',
  },
  rework: {
    eyebrow: 'Transmission Owner rework',
    title: 'Resubmit correction',
    description: 'Provide the revised correction in response to MISO feedback.',
    submitLabel: 'Resubmit correction',
    endpoint: id => `/api/dpp-reviews/${id}/corrections`,
    requiresComment: true,
    method: 'POST',
  },
  misoAccept: {
    eyebrow: 'MISO disposition',
    title: 'Accept correction?',
    description: 'This accepts the latest TO correction and completes the review.',
    submitLabel: 'Accept correction',
    endpoint: id => `/api/dpp-reviews/${id}/miso-accept`,
    requiresComment: false,
    method: 'POST',
  },
  misoReject: {
    eyebrow: 'MISO disposition',
    title: 'Return for rework',
    description: 'Explain what the Transmission Owner should revise before resubmitting.',
    submitLabel: 'Return for rework',
    endpoint: id => `/api/dpp-reviews/${id}/miso-reject`,
    requiresComment: true,
    method: 'POST',
  },
};

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function formatDate(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.valueOf())) return value;
  return new Intl.DateTimeFormat(undefined, {
    month: 'short', day: 'numeric', year: 'numeric', hour: 'numeric', minute: '2-digit'
  }).format(date);
}

function statusBadge(status) {
  const cfg = statusConfig[status] ?? { label: status, className: '' };
  return `<span class="status ${cfg.className}">${escapeHtml(cfg.label)}</span>`;
}

function setFeedback(message = '', tone = '') {
  els.feedback.textContent = message;
  els.feedback.className = `feedback ${tone}`.trim();
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...(options.headers ?? {}),
    },
  });

  const text = await response.text();
  let body = null;
  if (text) {
    try { body = JSON.parse(text); } catch { body = text; }
  }

  if (!response.ok) {
    const error = new Error(body?.message || body?.error || `Request failed with status ${response.status}`);
    error.status = response.status;
    error.body = body;
    throw error;
  }
  return body;
}

async function loadReviews({ silent = false } = {}) {
  if (!silent) setFeedback('Loading reviews…');
  els.refresh.disabled = true;
  try {
    state.reviews = await api('/api/dpp-reviews');
    renderSummary();
    renderReviews();
    setFeedback(`Showing ${state.reviews.length} review${state.reviews.length === 1 ? '' : 's'}.`);
  } catch (error) {
    setFeedback(`Unable to load reviews: ${error.message}`, 'error');
    els.list.innerHTML = '<div class="empty-state"><strong>Review data is unavailable.</strong><br>Confirm the collaboration API is healthy and try again.</div>';
  } finally {
    els.refresh.disabled = false;
  }
}

function renderSummary() {
  const counts = Object.fromEntries(Object.keys(statusConfig).map(status => [status, 0]));
  for (const review of state.reviews) counts[review.status] = (counts[review.status] ?? 0) + 1;
  const cards = [
    ['PENDING_TO_REVIEW', 'TO review', 'blue'],
    ['PENDING_MISO_REVIEW', 'MISO review', 'gold'],
    ['REWORK_REQUIRED', 'Rework required', 'green'],
    ['COMPLETED', 'Completed', 'muted'],
  ];
  els.summary.innerHTML = cards.map(([status, label, tone]) => `
    <button class="summary-card" data-tone="${tone}" data-filter-status="${status}" type="button">
      <strong>${counts[status] ?? 0}</strong><span>${label}</span>
    </button>`).join('');
}

function filteredReviews() {
  const query = els.search.value.trim().toLowerCase();
  const status = els.filter.value;
  return state.reviews
    .filter(r => status === 'ALL' || r.status === status)
    .filter(r => !query || `${r.dppResultId} ${r.transmissionOwnerId} ${r.reviewId}`.toLowerCase().includes(query))
    .sort((a, b) => new Date(b.updatedAt) - new Date(a.updatedAt));
}

function renderReviews() {
  const reviews = filteredReviews();
  if (!reviews.length) {
    els.list.innerHTML = '<div class="empty-state"><strong>No reviews match this view.</strong><br>Change the status filter or search criteria.</div>';
    return;
  }

  els.list.innerHTML = reviews.map(review => `
    <article class="review-row">
      <div>
        <span class="cell-label">DPP result</span>
        <h3 class="review-title">${escapeHtml(review.dppResultId)}</h3>
        <p class="review-meta">Review ${escapeHtml(review.reviewId)}</p>
      </div>
      <div><span class="cell-label">Transmission owner</span><strong>${escapeHtml(review.transmissionOwnerId)}</strong></div>
      <div><span class="cell-label">Status</span>${statusBadge(review.status)}</div>
      <div class="row-action"><button class="button secondary" type="button" data-review-id="${escapeHtml(review.reviewId)}">View</button></div>
    </article>`).join('');
}

async function openReview(id, { pushHash = true } = {}) {
  showView('detail');
  els.detailBody.innerHTML = '<div class="empty-state">Loading review…</div>';
  try {
    state.selected = await api(`/api/dpp-reviews/${id}`);
    renderDetail();
    if (pushHash) history.pushState(null, '', `#review/${id}`);
  } catch (error) {
    els.detailBody.innerHTML = `<div class="empty-state"><strong>Unable to load review.</strong><br>${escapeHtml(error.message)}</div>`;
  }
}

function actionPanel(review) {
  const status = review.status;
  if (status === 'PENDING_TO_REVIEW') {
    return actionPanelHtml('Transmission Owner action', 'Review the DPP result and either accept it or submit a correction.', [
      ['accept', 'Accept result', 'primary'], ['correction', 'Submit correction', 'secondary']
    ]);
  }
  if (status === 'PENDING_MISO_REVIEW') {
    return actionPanelHtml('MISO action', 'Review the latest TO correction and decide whether it resolves the issue.', [
      ['misoAccept', 'Accept correction', 'primary'], ['misoReject', 'Return for rework', 'secondary']
    ]);
  }
  if (status === 'REWORK_REQUIRED') {
    return actionPanelHtml('Transmission Owner action', 'MISO returned the correction for rework. Revise it and resubmit.', [
      ['rework', 'Resubmit correction', 'primary']
    ]);
  }
  return actionPanelHtml('Review complete', 'No further action is required for this DPP result.', []);
}

function actionPanelHtml(title, description, actions) {
  return `<section class="action-panel">
    <div><h3>${escapeHtml(title)}</h3><p>${escapeHtml(description)}</p></div>
    <div class="action-buttons">${actions.map(([action, label, tone]) => `<button class="button ${tone}" type="button" data-review-action="${action}">${escapeHtml(label)}</button>`).join('')}</div>
  </section>`;
}

function renderDetail() {
  const { review, corrections = [], dispositions = [] } = state.selected;
  const latestDisposition = dispositions.at(-1);
  els.detailBody.innerHTML = `
    <section class="detail-header">
      <div class="detail-title-row">
        <div><p class="eyebrow dark">DPP result review</p><h2 id="detail-title">${escapeHtml(review.dppResultId)}</h2><p class="section-copy">Transmission Owner: ${escapeHtml(review.transmissionOwnerId)}</p></div>
        ${statusBadge(review.status)}
      </div>
      <div class="detail-meta-grid">
        <div><span class="cell-label">Review ID</span><strong>${escapeHtml(review.reviewId)}</strong></div>
        <div><span class="cell-label">Process instance</span><strong>${escapeHtml(review.processInstanceKey ?? 'Not correlated')}</strong></div>
        <div><span class="cell-label">Created</span><strong>${escapeHtml(formatDate(review.createdAt))}</strong></div>
        <div><span class="cell-label">Last updated</span><strong>${escapeHtml(formatDate(review.updatedAt))}</strong></div>
      </div>
    </section>
    ${actionPanel(review)}
    <div class="history-grid">
      <section class="history-card card">
        <h3>Correction history</h3>
        ${renderCorrections(corrections)}
      </section>
      <section class="history-card card">
        <h3>MISO dispositions</h3>
        ${renderDispositions(dispositions, latestDisposition)}
      </section>
    </div>`;
}

function renderCorrections(corrections) {
  if (!corrections.length) return '<p class="section-copy">No corrections have been submitted.</p>';
  return `<ol class="timeline">${corrections.slice().reverse().map(c => `<li><strong>Correction v${escapeHtml(c.version)}</strong><p>${escapeHtml(c.comment)}</p><small>${escapeHtml(formatDate(c.createdAt))}</small></li>`).join('')}</ol>`;
}

function renderDispositions(dispositions) {
  if (!dispositions.length) return '<p class="section-copy">No MISO disposition has been recorded.</p>';
  return `<ol class="timeline">${dispositions.slice().reverse().map(d => {
    const decision = d.decision === 'ACCEPT_CORRECTION' ? 'Accepted correction' : 'Returned for rework';
    return `<li><strong>${escapeHtml(decision)} · v${escapeHtml(d.correctionVersion)}</strong>${d.comment ? `<p>${escapeHtml(d.comment)}</p>` : ''}<small>${escapeHtml(formatDate(d.createdAt))}</small></li>`;
  }).join('')}</ol>`;
}

function openAction(actionName) {
  const cfg = actionConfig[actionName];
  if (!cfg || !state.selected?.review) return;
  state.action = {
    name: actionName,
    key: crypto.randomUUID(),
    reviewId: state.selected.review.reviewId,
    lastComment: '',
  };
  els.dialogEyebrow.textContent = cfg.eyebrow;
  els.dialogTitle.textContent = cfg.title;
  els.dialogDescription.textContent = cfg.description;
  els.dialogSubmit.textContent = cfg.submitLabel;
  els.dialogComment.value = '';
  els.dialogComment.required = cfg.requiresComment;
  els.commentField.classList.toggle('hidden', !cfg.requiresComment);
  els.dialog.showModal();
  if (cfg.requiresComment) els.dialogComment.focus(); else els.dialogSubmit.focus();
}

async function submitAction(event) {
  event.preventDefault();
  if (!state.action || !state.selected) return;
  const cfg = actionConfig[state.action.name];
  const comment = els.dialogComment.value.trim();
  if (cfg.requiresComment && !comment) {
    els.dialogComment.focus();
    return;
  }

  if (state.action.lastComment !== '' && state.action.lastComment !== comment) {
    state.action.key = crypto.randomUUID();
  }
  state.action.lastComment = comment;
  els.dialogSubmit.disabled = true;
  els.dialogSubmit.textContent = 'Submitting…';

  try {
    const options = {
      method: cfg.method,
      headers: { 'Idempotency-Key': state.action.key },
    };
    if (cfg.requiresComment) options.body = JSON.stringify({ comment });
    const result = await api(cfg.endpoint(state.action.reviewId), options);
    els.dialog.close();
    state.action = null;
    state.selected = result;
    renderDetail();
    await loadReviews({ silent: true });
  } catch (error) {
    const recovery = error.status === 502 ? ' The business update may already be committed; retry this same action to reconcile workflow state.' : '';
    els.dialogDescription.textContent = `${cfg.description} Error: ${error.message}.${recovery}`;
    els.dialogDescription.style.color = 'var(--red-600)';
    // Keep the same idempotency key on 502 so the user can safely retry the exact action.
    if (error.status !== 502) state.action.key = crypto.randomUUID();
  } finally {
    els.dialogSubmit.disabled = false;
    els.dialogSubmit.textContent = cfg.submitLabel;
  }
}

function showView(view) {
  els.queue.classList.toggle('hidden', view !== 'queue');
  els.detail.classList.toggle('hidden', view !== 'detail');
  els.about.classList.toggle('hidden', view !== 'about');
  document.querySelectorAll('.nav-link').forEach(link => link.classList.toggle('active', link.dataset.view === view));
}

function goHome({ replace = false } = {}) {
  showView('queue');
  state.selected = null;
  if (replace) history.replaceState(null, '', '#reviews'); else history.pushState(null, '', '#reviews');
}

function handleHash() {
  const hash = location.hash || '#reviews';
  if (hash.startsWith('#review/')) {
    openReview(hash.slice('#review/'.length), { pushHash: false });
  } else if (hash === '#about') {
    showView('about');
  } else {
    showView('queue');
  }
}

els.refresh.addEventListener('click', () => loadReviews());
els.search.addEventListener('input', renderReviews);
els.filter.addEventListener('change', renderReviews);
els.back.addEventListener('click', () => goHome());
els.dialogCancel.addEventListener('click', () => { state.action = null; els.dialog.close(); });
els.actionForm.addEventListener('submit', submitAction);
els.dialog.addEventListener('close', () => {
  if (els.dialog.returnValue === 'cancel') state.action = null;
  els.dialogDescription.style.color = '';
});

els.list.addEventListener('click', event => {
  const button = event.target.closest('[data-review-id]');
  if (button) openReview(button.dataset.reviewId);
});
els.summary.addEventListener('click', event => {
  const button = event.target.closest('[data-filter-status]');
  if (!button) return;
  els.filter.value = button.dataset.filterStatus;
  renderReviews();
});
els.detailBody.addEventListener('click', event => {
  const button = event.target.closest('[data-review-action]');
  if (button) openAction(button.dataset.reviewAction);
});
document.querySelectorAll('[data-view]').forEach(button => button.addEventListener('click', () => {
  const view = button.dataset.view;
  if (view === 'queue') goHome();
  if (view === 'about') { showView('about'); history.pushState(null, '', '#about'); }
}));
document.querySelector('[data-action="home"]').addEventListener('click', () => goHome());
window.addEventListener('popstate', handleHash);

loadReviews({ silent: true }).then(handleHash);
