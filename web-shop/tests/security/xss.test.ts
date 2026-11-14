import { mount } from '@vue/test-utils';
import { NoticeBar } from 'vant';
import { describe, expect, it } from 'vitest';

describe('shop XSS protection', () => {
  it('renders notice text without executing markup', () => {
    const payload = '<img src=x onerror="alert(1)"><script>alert(2)</script>';
    const wrapper = mount(NoticeBar, {
      props: {
        text: payload,
      },
    });

    expect(wrapper.text()).toContain(payload);
    expect(wrapper.find('img').exists()).toBe(false);
    expect(wrapper.find('script').exists()).toBe(false);
  });
});
