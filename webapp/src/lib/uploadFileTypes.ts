export const SUPPORTED_UPLOAD_EXTENSIONS = ['.txt', '.md', '.markdown', '.pdf', '.docx', '.pptx'] as const;

export const SUPPORTED_UPLOAD_ACCEPT = SUPPORTED_UPLOAD_EXTENSIONS.join(',');

export const SUPPORTED_UPLOAD_PLACEHOLDER_TEXT = 'PDF, TXT, Markdown, DOCX, PPTX 파일들을 드래그하세요';

type PartitionedUploadFiles = {
  supported: File[];
  unsupportedNames: string[];
};

/**
 * 업로드 가능한 확장자 기준으로 파일 목록을 분리한다.
 */
export function partitionSupportedUploadFiles(files: File[]): PartitionedUploadFiles {
  const supported: File[] = [];
  const unsupportedNames: string[] = [];

  files.forEach((file) => {
    const lower = file.name.toLowerCase();
    const matched = SUPPORTED_UPLOAD_EXTENSIONS.some((extension) => lower.endsWith(extension));
    if (matched) {
      supported.push(file);
      return;
    }
    unsupportedNames.push(file.name);
  });

  return { supported, unsupportedNames };
}
