import globalAxios from "axios";
import { BASE_PATH } from "../generated-skeleton-api/base";

export const ReportService = {
    /**
     * Sends the generated PDF report to the given recipient email.
     * Uses the global axios instance so the existing Bearer token interceptor
     * is applied automatically — no manual token handling needed.
     */
    sendReportEmail: async (recipientEmail: string, pdfBlob: Blob, filename?: string): Promise<void> => {
        const resolvedFilename =
            filename ?? `room-climate-report-${new Date().toISOString().slice(0, 16).replace("T", "-").replace(":", "")}.pdf`;

        const form = new FormData();
        form.append("to", recipientEmail);
        form.append("attachment", pdfBlob, resolvedFilename);

        await globalAxios.post(`${BASE_PATH}/api/reports/send`, form);
    },
};