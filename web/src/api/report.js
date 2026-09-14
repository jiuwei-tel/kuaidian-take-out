import request from './request'

/**
 * 四个报表接口返回的都是「逗号分隔字符串」，不是数组：
 *   TurnoverReportVO  { dateList, turnoverList }
 *   UserReportVO      { dateList, totalUserList, newUserList }
 *   OrderReportVO     { dateList, orderCountList, validOrderCountList,
 *                       totalOrderCount, validOrderCount, orderCompletionRate }
 *   SalesTop10ReportVO{ nameList, numberList }
 * 用 utils/format.js 里的 splitList / splitNumList 拆。
 */

/** 营业额统计 */
export function turnoverStatistics(begin, end) {
  return request({
    url: '/admin/report/turnoverStatistics',
    method: 'get',
    params: { begin, end },
  })
}

/** 用户统计 */
export function userStatistics(begin, end) {
  return request({
    url: '/admin/report/userStatistics',
    method: 'get',
    params: { begin, end },
  })
}

/** 订单统计 */
export function ordersStatistics(begin, end) {
  return request({
    url: '/admin/report/ordersStatistics',
    method: 'get',
    params: { begin, end },
  })
}

/** 销量排名 Top10 */
export function salesTop10(begin, end) {
  return request({
    url: '/admin/report/top10',
    method: 'get',
    params: { begin, end },
  })
}
