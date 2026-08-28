import {signIn, getCsrfToken, authHeaders} from './helpers/auth.js';
import {TEST_USER_EMAILS, TEST_USER_PASSWORD} from './config/env.js';
import http from 'k6/http';
import {check, sleep} from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://nginx';
const BURST_ITERATIONS = Number(__ENV.BURST_ITERATIONS || 30);
const PROBE_VUS = Number(__ENV.PROBE_VUS || 3);
const BEFORE_S = 20;
const DURING_S = 180;
const AFTER_S = 20;

export const options = {
  scenarios: {
    probe_before: {
      executor: 'constant-vus',
      vus: PROBE_VUS,
      duration: `${BEFORE_S}s`,
      exec: 'probe',
      tags: {phase: 'before'},
    },
    burst: {
      executor: 'shared-iterations',
      vus: 1,
      iterations: BURST_ITERATIONS,
      maxDuration: '5m',
      startTime: `${BEFORE_S}s`,
      exec: 'burst',
    },
    probe_during: {
      executor: 'constant-vus',
      vus: PROBE_VUS,
      duration: `${DURING_S}s`,
      startTime: `${BEFORE_S}s`,
      exec: 'probe',
      tags: {phase: 'during'},
    },
    probe_after: {
      executor: 'constant-vus',
      vus: PROBE_VUS,
      duration: `${AFTER_S}s`,
      startTime: `${BEFORE_S + DURING_S}s`,
      exec: 'probe',
      tags: {phase: 'after'},
    },
  },
  thresholds: {
    'http_req_duration{phase:before}': ['p(95)<5000'],
    'http_req_duration{phase:during}': ['p(95)<5000'],
    'http_req_duration{phase:after}': ['p(95)<5000'],
  },
};

export function setup() {
  const probeSession = signIn(BASE_URL, TEST_USER_EMAILS[1], TEST_USER_PASSWORD);
  if (!probeSession.ok) {
    throw new Error(`probe 로그인 실패: ${probeSession.response.status} ${probeSession.response.body}`);
  }
  const burstSession = signIn(BASE_URL, TEST_USER_EMAILS[0], TEST_USER_PASSWORD);
  if (!burstSession.ok) {
    throw new Error(`burst 로그인 실패: ${burstSession.response.status} ${burstSession.response.body}`);
  }
  return {probeSession, burstSession};
}

export function probe(data) {
  const res = http.get(
      `${BASE_URL}/api/users/${data.probeSession.userId}/profiles`,
      {headers: authHeaders(data.probeSession), tags: {name: 'probe_profile'}}
  );
  check(res, {'probe 200': (r) => r.status === 200});
  sleep(0.5);
}

export function burst(data) {
  const name = `executor-saturation-${Date.now()}-${__ITER}`;
  const csrf = getCsrfToken(BASE_URL);
  const res = http.post(
      `${BASE_URL}/api/clothes/attribute-defs`,
      JSON.stringify({name, selectableValues: ['A', 'B', 'C']}),
      {
        headers: authHeaders(data.burstSession, {'Content-Type': 'application/json', 'X-XSRF-TOKEN': csrf}),
        tags: {name: 'burst_create'},
      }
  );
  check(res, {'burst create 201': (r) => r.status === 201});
  console.log(`BURST_ITER=${__ITER} STATUS=${res.status} NAME=${name}`);
}
