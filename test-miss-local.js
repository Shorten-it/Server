import http from 'k6/http';
import { check } from 'k6';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export const options = {
  vus: 20,
  duration: '30s',
};

const BASE_URL = 'http://localhost:8080/api/v1/url';

export default function () {
  const randomShortCode = uuidv4().substring(0, 8);
  const res = http.get(`${BASE_URL}/${randomShortCode}`);

  check(res, {
    'status is error (>=400)': (r) => r.status >= 400,
  });
}


